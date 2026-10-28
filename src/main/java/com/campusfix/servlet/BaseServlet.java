package com.campusfix.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseServlet extends HttpServlet {

    protected final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    protected void sendJsonResponse(HttpServletResponse resp, int statusCode, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(statusCode);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(data));
            out.flush();
        }
    }

    protected void sendSuccess(HttpServletResponse resp, String message, Object payload) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", message);
        if (payload != null) {
            map.put("data", payload);
        }
        sendJsonResponse(resp, HttpServletResponse.SC_OK, map);
    }

    protected void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("error", message);
        sendJsonResponse(resp, statusCode, map);
    }

    protected <T> T parseJsonBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), clazz);
    }
}
