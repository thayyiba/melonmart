package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.model.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        User user = getLoggedInUser(request);

        // Not logged in
        if (user == null) {

            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Login required."
            );

            return;
        }

        // Logged in but not an admin
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {

            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Admin access required."
            );

            return;
        }

        Map<String, Object> result =
                new HashMap<>();

        result.put("success", true);
        result.put("message", "Admin access granted.");
        result.put("admin", user.getUsername());
        result.put("role", user.getRole());

        sendJson(
                response,
                HttpServletResponse.SC_OK,
                result
        );
    }

    private User getLoggedInUser(
            HttpServletRequest request
    ) {

        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return null;
        }

        Object sessionUser =
                session.getAttribute("user");

        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }

        return null;
    }

    private void sendJson(
            HttpServletResponse response,
            int status,
            Object data
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        response.getWriter().write(
                gson.toJson(data)
        );
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        Map<String, Object> result =
                new HashMap<>();

        result.put("success", false);
        result.put("message", message);

        sendJson(
                response,
                status,
                result
        );
    }
}