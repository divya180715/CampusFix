package com.campusfix.servlet;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.dao.UserDAO;
import com.campusfix.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/admin/stats", "/api/admin/users", "/api/admin/staff", "/api/meta"})
public class AdminServlet extends BaseServlet {

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final UserDAO userDAO = new UserDAO();

    private static final String[] CATEGORIES = new String[]{
            "Infrastructure", "IT Services", "Academic", "Hostel/Accommodation",
            "Faculty Conduct", "Administration", "Library", "Fees/Finance",
            "Examination", "Harassment/Ragging", "Other"
    };

    private static final String[] STATUSES = new String[]{
            "Pending", "Assigned", "In Progress", "Resolved", "Rejected"
    };

    private static final String[] PRIORITIES = new String[]{
            "Low", "Medium", "High", "Urgent"
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();

        if ("/api/meta".equals(path)) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("categories", CATEGORIES);
            meta.put("statuses", STATUSES);
            meta.put("priorities", PRIORITIES);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, meta);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        if ("/api/admin/staff".equals(path)) {
            // Accessible to admins for assignment dropdowns
            List<User> staff = userDAO.listStaffAndAdmins();
            Map<String, Object> payload = new HashMap<>();
            payload.put("staff", staff);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, payload);
            return;
        }

        if (!"admin".equalsIgnoreCase(user.getRole())) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required");
            return;
        }

        if ("/api/admin/stats".equals(path)) {
            Map<String, Object> stats = complaintDAO.getDashboardStats();
            sendJsonResponse(resp, HttpServletResponse.SC_OK, stats);
        } else if ("/api/admin/users".equals(path)) {
            List<User> users = userDAO.listUsers();
            Map<String, Object> payload = new HashMap<>();
            payload.put("users", users);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, payload);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }
}
