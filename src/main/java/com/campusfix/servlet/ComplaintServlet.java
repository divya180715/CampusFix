package com.campusfix.servlet;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.model.Complaint;
import com.campusfix.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/complaints", "/api/complaints/*"})
public class ComplaintServlet extends BaseServlet {

    private final ComplaintDAO complaintDAO = new ComplaintDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            // List complaints with optional query filters
            String status = req.getParameter("status");
            String category = req.getParameter("category");
            String search = req.getParameter("search");

            List<Complaint> complaints = complaintDAO.listComplaints(
                    user.getRole(),
                    user.getId(),
                    status,
                    category,
                    search
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("complaints", complaints);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, payload);
            return;
        }

        // Single complaint: /api/complaints/{id}
        try {
            int complaintId = Integer.parseInt(pathInfo.substring(1));
            Complaint complaint = complaintDAO.getComplaintById(complaintId);
            if (complaint == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Complaint not found");
                return;
            }

            // Authorization: admin & maintenance can view all; student/faculty only their own
            if (!"admin".equalsIgnoreCase(user.getRole()) &&
                !"maintenance".equalsIgnoreCase(user.getRole()) &&
                complaint.getFiledBy() != user.getId()) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Not authorized to view this complaint");
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("complaint", complaint);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, payload);
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid complaint ID");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = parseJsonBody(req, Map.class);
        if (body == null) body = new HashMap<>();

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String category = (String) body.get("category");
        String priority = (String) body.get("priority");
        Boolean isAnonymous = (Boolean) body.get("is_anonymous");

        if (title == null || title.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title and description are required");
            return;
        }

        Complaint complaint = new Complaint();
        complaint.setTitle(title.trim());
        complaint.setDescription(description.trim());
        complaint.setCategory(category != null ? category.trim() : "Other");
        complaint.setPriority(priority != null ? priority.trim() : "Medium");
        complaint.setAnonymous(isAnonymous != null && isAnonymous);
        complaint.setFiledBy(user.getId());
        complaint.setFiledByRole(user.getRole());

        boolean created = complaintDAO.createComplaint(complaint);
        if (created) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("complaint", complaint);
            sendJsonResponse(resp, HttpServletResponse.SC_CREATED, payload);
        } else {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to file complaint");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPatchOrPut(req, resp);
    }

    // Handles status, assignment and priority updates
    private void doPatchOrPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        // Only admin and maintenance can update complaint status or assignments
        if (!"admin".equalsIgnoreCase(user.getRole()) && !"maintenance".equalsIgnoreCase(user.getRole())) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only admin or maintenance staff can update complaints");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing complaint ID in URL path");
            return;
        }

        try {
            // Path could be /12 or /12/status
            String rawId = pathInfo.substring(1);
            if (rawId.contains("/")) {
                rawId = rawId.substring(0, rawId.indexOf("/"));
            }
            int complaintId = Integer.parseInt(rawId);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = parseJsonBody(req, Map.class);
            if (body == null) body = new HashMap<>();

            String status = (String) body.get("status");
            String adminNotes = (String) body.get("admin_notes");
            String priority = (String) body.get("priority");

            Integer assignedTo = null;
            if (body.containsKey("assigned_to") && body.get("assigned_to") != null) {
                if (body.get("assigned_to") instanceof Number) {
                    assignedTo = ((Number) body.get("assigned_to")).intValue();
                } else {
                    assignedTo = Integer.parseInt(body.get("assigned_to").toString());
                }
            }

            boolean updated = complaintDAO.updateStatusAndAssignment(complaintId, status, assignedTo, adminNotes, priority);
            if (updated) {
                Complaint fresh = complaintDAO.getComplaintById(complaintId);
                Map<String, Object> payload = new HashMap<>();
                payload.put("complaint", fresh);
                sendSuccess(resp, "Complaint updated successfully", payload);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Complaint not found or update failed");
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid complaint ID format");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing complaint ID");
            return;
        }

        try {
            int complaintId = Integer.parseInt(pathInfo.substring(1));
            Complaint existing = complaintDAO.getComplaintById(complaintId);
            if (existing == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Complaint not found");
                return;
            }

            // Only admin or the author (while pending) can delete
            if (!"admin".equalsIgnoreCase(user.getRole()) &&
                (existing.getFiledBy() != user.getId() || !"Pending".equalsIgnoreCase(existing.getStatus()))) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Cannot delete complaint");
                return;
            }

            boolean deleted = complaintDAO.deleteComplaint(complaintId);
            if (deleted) {
                sendSuccess(resp, "Complaint deleted successfully", null);
            } else {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Delete failed");
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid complaint ID");
        }
    }
}
