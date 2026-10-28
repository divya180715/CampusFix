package com.campusfix.dao;

import com.campusfix.model.Complaint;
import com.campusfix.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplaintDAO {

    public boolean createComplaint(Complaint complaint) {
        String sql = "INSERT INTO complaints (title, description, category, priority, status, is_anonymous, filed_by, filed_by_role) " +
                     "VALUES (?, ?, ?, ?, 'Pending', ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, complaint.getTitle());
            ps.setString(2, complaint.getDescription());
            ps.setString(3, complaint.getCategory());
            ps.setString(4, complaint.getPriority() != null ? complaint.getPriority() : "Medium");
            ps.setInt(5, complaint.isAnonymous() ? 1 : 0);
            ps.setInt(6, complaint.getFiledBy());
            ps.setString(7, complaint.getFiledByRole());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        complaint.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Complaint> listComplaints(String role, int userId, String status, String category, String search) {
        List<Complaint> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, " +
                "  CASE WHEN c.is_anonymous = 1 THEN 'Anonymous' ELSE u.name END AS filed_by_name, " +
                "  a.name AS assigned_to_name " +
                "FROM complaints c " +
                "LEFT JOIN users u ON c.filed_by = u.id " +
                "LEFT JOIN users a ON c.assigned_to = a.id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if ("student".equalsIgnoreCase(role) || "faculty".equalsIgnoreCase(role)) {
            sql.append(" AND c.filed_by = ? ");
            params.add(userId);
        } else if ("maintenance".equalsIgnoreCase(role)) {
            // Maintenance staff sees complaints assigned to them or unassigned pending in progress
            sql.append(" AND (c.assigned_to = ? OR c.assigned_to IS NULL) ");
            params.add(userId);
        }
        // admin sees everything

        if (status != null && !status.isEmpty() && !"All".equalsIgnoreCase(status)) {
            sql.append(" AND c.status = ? ");
            params.add(status);
        }
        if (category != null && !category.isEmpty() && !"All".equalsIgnoreCase(category)) {
            sql.append(" AND c.category = ? ");
            params.add(category);
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (c.title LIKE ? OR c.description LIKE ?) ");
            String likePattern = "%" + search.trim() + "%";
            params.add(likePattern);
            params.add(likePattern);
        }

        sql.append(" ORDER BY c.created_at DESC");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToComplaint(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Complaint getComplaintById(int id) {
        String sql = "SELECT c.*, " +
                "  CASE WHEN c.is_anonymous = 1 THEN 'Anonymous' ELSE u.name END AS filed_by_name, " +
                "  a.name AS assigned_to_name " +
                "FROM complaints c " +
                "LEFT JOIN users u ON c.filed_by = u.id " +
                "LEFT JOIN users a ON c.assigned_to = a.id " +
                "WHERE c.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToComplaint(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateStatusAndAssignment(int complaintId, String status, Integer assignedTo, String adminNotes, String priority) {
        StringBuilder sql = new StringBuilder("UPDATE complaints SET ");
        List<Object> params = new ArrayList<>();
        boolean first = true;

        if (status != null && !status.isEmpty()) {
            sql.append("status = ? ");
            params.add(status);
            if ("Resolved".equalsIgnoreCase(status)) {
                sql.append(", resolved_at = CURRENT_TIMESTAMP ");
            }
            first = false;
        }

        if (assignedTo != null) {
            if (!first) sql.append(", ");
            sql.append("assigned_to = ? ");
            params.add(assignedTo == 0 ? null : assignedTo);
            first = false;
        }

        if (adminNotes != null) {
            if (!first) sql.append(", ");
            sql.append("admin_notes = ? ");
            params.add(adminNotes);
            first = false;
        }

        if (priority != null && !priority.isEmpty()) {
            if (!first) sql.append(", ");
            sql.append("priority = ? ");
            params.add(priority);
            first = false;
        }

        sql.append(" WHERE id = ?");
        params.add(complaintId);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteComplaint(int complaintId) {
        String sql = "DELETE FROM complaints WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Integer> byStatus = new HashMap<>();
        Map<String, Integer> byCategory = new HashMap<>();

        try (Connection conn = DBUtil.getConnection()) {
            // Total count
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM complaints");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("total", rs.getInt("total"));
                }
            }

            // By status
            try (PreparedStatement ps = conn.prepareStatement("SELECT status, COUNT(*) AS count FROM complaints GROUP BY status");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byStatus.put(rs.getString("status"), rs.getInt("count"));
                }
            }

            // By category
            try (PreparedStatement ps = conn.prepareStatement("SELECT category, COUNT(*) AS count FROM complaints GROUP BY category");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byCategory.put(rs.getString("category"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        stats.put("by_status", byStatus);
        stats.put("by_category", byCategory);
        return stats;
    }

    private Complaint mapRowToComplaint(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setId(rs.getInt("id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setCategory(rs.getString("category"));
        c.setPriority(rs.getString("priority"));
        c.setStatus(rs.getString("status"));
        c.setAnonymous(rs.getInt("is_anonymous") == 1);
        c.setFiledBy(rs.getInt("filed_by"));
        c.setFiledByRole(rs.getString("filed_by_role"));

        try {
            c.setFiledByName(rs.getString("filed_by_name"));
        } catch (SQLException ignored) {}

        int assignedToVal = rs.getInt("assigned_to");
        if (rs.wasNull()) {
            c.setAssignedTo(null);
        } else {
            c.setAssignedTo(assignedToVal);
        }

        try {
            c.setAssignedToName(rs.getString("assigned_to_name"));
        } catch (SQLException ignored) {}

        c.setAdminNotes(rs.getString("admin_notes"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        c.setResolvedAt(rs.getTimestamp("resolved_at"));
        return c;
    }
}
