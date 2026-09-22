package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.dao.UserDAO;
import com.melon.melonmart.model.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet({"/api/auth/*", "/api/v1/auth/*"})
public class AuthServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/login".equals(path)) handleLogin(req, resp);
        else if ("/register".equals(path)) handleRegister(req, resp);
        else if ("/logout".equals(path)) handleLogout(req, resp);
        else sendError(resp, 404, "Authentication endpoint not found.");
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/me".equals(req.getPathInfo())) handleCurrentUser(req, resp);
        else sendError(resp, 404, "Authentication endpoint not found.");
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String,String> data = readJson(req);
        String email = data.get("email");
        String password = data.get("password");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            sendError(resp, 400, "Email and password are required."); return;
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null || user.getPassword() == null || !BCrypt.checkpw(password, user.getPassword())) {
            sendError(resp, 401, "Invalid email or password."); return;
        }

        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute("user", user);
        sendUserResponse(resp, user, "Login successful.", 200);
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String,String> data = readJson(req);
        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");
        String role = data.get("role");
        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.length() < 6) {
            sendError(resp, 400, "Name, email and a password of at least 6 characters are required."); return;
        }
        // Never trust a role sent by the browser. Registration can create only BUYER or SELLER.
        role = "SELLER".equalsIgnoreCase(role) ? "SELLER" : "BUYER";
        email = email.trim().toLowerCase();
        if (userDAO.findByEmail(email) != null) {
            sendError(resp, 409, "An account with this email already exists."); return;
        }
        User user = new User(0, name.trim(), email, password, role);
        if (!userDAO.registerUser(user)) {
            sendError(resp, 500, "Registration failed."); return;
        }
        Map<String,Object> out = new HashMap<>();
        out.put("success", true); out.put("message", "Account created successfully.");
        sendJson(resp, 201, out);
    }

    private void handleCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            Map<String,Object> out = new HashMap<>(); out.put("success", false); out.put("authenticated", false);
            sendJson(resp, 401, out); return;
        }
        Map<String,Object> out = new HashMap<>();
        out.put("success", true);
        out.put("authenticated", true);
        Map<String,Object> safe = safeUser(user);
        out.put("user", safe);
        // Keep user/role fields at the top level for existing pages that consume /api/auth/me.
        out.putAll(safe);
        sendJson(resp, 200, out);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        Map<String,Object> out = new HashMap<>(); out.put("success", true); out.put("message", "Logged out successfully.");
        sendJson(resp, 200, out);
    }

    private Map<String,Object> safeUser(User user) {
        Map<String,Object> out = new HashMap<>();
        out.put("id", user.getId()); out.put("name", user.getUsername()); out.put("username", user.getUsername());
        out.put("email", user.getEmail()); out.put("role", user.getRole());
        return out;
    }

    private void sendUserResponse(HttpServletResponse resp, User user, String message, int status) throws IOException {
        Map<String,Object> out = new HashMap<>(); out.put("success", true); out.put("message", message); out.put("user", safeUser(user));
        out.putAll(safeUser(user)); sendJson(resp, status, out);
    }

    private Map<String,String> readJson(HttpServletRequest req) throws IOException {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = req.getReader()) { String line; while ((line = reader.readLine()) != null) json.append(line); }
        Map<String,String> data = gson.fromJson(json.toString(), Map.class);
        return data == null ? new HashMap<>() : data;
    }

    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setContentType("application/json"); resp.setCharacterEncoding("UTF-8"); resp.setStatus(status);
        resp.getWriter().write(gson.toJson(body));
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        Map<String,Object> out = new HashMap<>(); out.put("success", false); out.put("message", message); sendJson(resp, status, out);
    }
}
