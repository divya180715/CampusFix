package com.campusfix.servlet;

import com.campusfix.dao.UserDAO;
import com.campusfix.model.User;
import com.campusfix.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/auth/me"})
public class AuthServlet extends BaseServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        if ("/api/auth/me".equals(path)) {
            handleMe(req, resp);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        switch (path) {
            case "/api/auth/login":
                handleLogin(req, resp);
                break;
            case "/api/auth/register":
                handleRegister(req, resp);
                break;
            case "/api/auth/logout":
                handleLogout(req, resp);
                break;
            default:
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, String> creds = parseJsonBody(req, Map.class);
        if (creds == null) creds = new HashMap<>();

        String email = creds.get("email");
        String password = creds.get("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required");
            return;
        }

        User user = userDAO.findByEmail(email.toLowerCase().trim());
        if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole());
        session.setAttribute("name", user.getName());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("department", user.getDepartment());
        data.put("idNumber", user.getIdNumber());

        sendSuccess(resp, "Login successful", data);
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, String> body = parseJsonBody(req, Map.class);
        if (body == null) body = new HashMap<>();

        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String role = body.get("role");
        String department = body.get("department");
        String idNumber = body.get("id_number");

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            role == null || role.trim().isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name, email, password, and role are required");
            return;
        }

        if (userDAO.findByEmail(email.toLowerCase().trim()) != null) {
            sendError(resp, HttpServletResponse.SC_CONFLICT, "An account with this email already exists");
            return;
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.toLowerCase().trim());
        user.setRole(role.trim());
        user.setDepartment(department != null ? department.trim() : "");
        user.setIdNumber(idNumber != null ? idNumber.trim() : "");

        boolean created = userDAO.registerUser(user, password);
        if (created) {
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("role", user.getRole());
            session.setAttribute("name", user.getName());

            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("name", user.getName());
            data.put("email", user.getEmail());
            data.put("role", user.getRole());
            data.put("department", user.getDepartment());

            sendSuccess(resp, "Account registered successfully", data);
        } else {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed, please check inputs");
        }
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("user", null);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, empty);
            return;
        }

        User user = (User) session.getAttribute("user");
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("department", user.getDepartment());

        Map<String, Object> result = new HashMap<>();
        result.put("user", data);
        sendJsonResponse(resp, HttpServletResponse.SC_OK, result);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        sendSuccess(resp, "Logged out successfully", null);
    }
}
