package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.listener.DbContextListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@WebServlet({"/api/health", "/api/v1/health"})
public class HealthServlet extends HttpServlet {
    private final Gson gson = new Gson();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String,Object> data = new HashMap<>(); data.put("status","UP");
        try (Connection c = DbContextListener.getDataSource().getConnection()) {
            data.put("db", c.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) { data.put("db","DOWN"); data.put("status","DOWN"); }
        resp.setContentType("application/json"); resp.setCharacterEncoding("UTF-8");
        resp.setStatus("UP".equals(data.get("status")) ? 200 : 503); resp.getWriter().write(gson.toJson(data));
    }
}
