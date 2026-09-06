package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.dao.UserDAO;
import com.melon.melonmart.model.User;

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

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String path = request.getPathInfo();

        if ("/login".equals(path)) {
            handleLogin(request, response);
        } else if ("/register".equals(path)) {
            handleRegister(request, response);
        } else if ("/logout".equals(path)) {
            handleLogout(request, response);
        } else {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "Authentication endpoint not found."
            );
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String path = request.getPathInfo();

        if ("/me".equals(path)) {
            handleCurrentUser(request, response);
        } else {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "Authentication endpoint not found."
            );
        }
    }

    private void handleLogin(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        Map<String, String> data = readJson(request);

        String email = data.get("email");
        String password = data.get("password");

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Email and password are required."
            );
            return;
        }

        User user = userDAO.findByEmail(email);

        if (user == null || !user.getPassword().equals(password)) {

            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid email or password."
            );
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("message", "Login successful.");
        result.put("user", user);

        // Also provide user information at the top level
        result.put("id", user.getId());
        result.put("name", user.getUsername());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());

        response.getWriter().write(gson.toJson(result));
    }

    private void handleRegister(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        Map<String, String> data = readJson(request);

        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");
        String role = data.get("role");

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Name, email and password are required."
            );
            return;
        }

        if (!"SELLER".equalsIgnoreCase(role)
                && !"BUYER".equalsIgnoreCase(role)) {

            role = "BUYER";

        } else {
            role = role.toUpperCase();
        }

        if (userDAO.findByEmail(email) != null) {

            sendError(
                    response,
                    HttpServletResponse.SC_CONFLICT,
                    "An account with this email already exists."
            );
            return;
        }

        User user = new User(
                0,
                name,
                email,
                password,
                role
        );

        boolean registered = userDAO.registerUser(user);

        if (!registered) {

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Registration failed."
            );
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_CREATED);

        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("message", "Account created successfully.");

        response.getWriter().write(gson.toJson(result));
    }

    private void handleCurrentUser(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        HttpSession session = request.getSession(false);

        User user = null;

        if (session != null) {
            Object sessionUser = session.getAttribute("user");

            if (sessionUser instanceof User) {
                user = (User) sessionUser;
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (user == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            Map<String, Object> result = new HashMap<>();

            result.put("authenticated", false);

            response.getWriter().write(
                    gson.toJson(result)
            );

            return;
        }

        response.setStatus(
                HttpServletResponse.SC_OK
        );

        Map<String, Object> result = new HashMap<>();

        result.put("authenticated", true);

        // Original nested user object
        result.put("user", user);

        // Top-level fields for frontend compatibility
        result.put("id", user.getId());
        result.put("name", user.getUsername());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());

        response.getWriter().write(
                gson.toJson(result)
        );
    }

    private void handleLogout(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put(
                "message",
                "Logged out successfully."
        );

        response.getWriter().write(
                gson.toJson(result)
        );
    }

    private Map<String, String> readJson(
            HttpServletRequest request
    ) throws IOException {

        StringBuilder json =
                new StringBuilder();

        try (BufferedReader reader =
                     request.getReader()) {

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        Map<String, String> data =
                gson.fromJson(
                        json.toString(),
                        Map.class
                );

        return data != null
                ? data
                : new HashMap<>();
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        Map<String, Object> result =
                new HashMap<>();

        result.put("success", false);
        result.put("message", message);

        response.getWriter().write(
                gson.toJson(result)
        );
    }
}