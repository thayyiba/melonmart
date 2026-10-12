package com.melon.melonmart.controller;

import com.google.gson.Gson;
import com.melon.melonmart.dao.DeliveryAddressDAO;
import com.melon.melonmart.model.DeliveryAddress;
import com.melon.melonmart.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet({"/api/addresses", "/api/v1/addresses"})
public class DeliveryAddressServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private DeliveryAddressDAO addressDAO;

    @Override
    public void init() throws ServletException {
        addressDAO = new DeliveryAddressDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = getLoggedInUser(request);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to access saved addresses.");
            return;
        }
        try {
            sendJson(response, HttpServletResponse.SC_OK, addressDAO.findByUserId(user.getId()));
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load saved addresses.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = getLoggedInUser(request);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to save an address.");
            return;
        }

        try {
            Map<String, Object> data = readJson(request);
            String action = stringValue(data.get("action"));
            if ("select".equals(action)) {
                int id = integerValue(data.get("id"));
                if (id <= 0 || !addressDAO.setDefault(user.getId(), id)) {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Saved address not found.");
                    return;
                }
                sendJson(response, HttpServletResponse.SC_OK, success("Default delivery address updated."));
                return;
            }

            if (!"create".equals(action)) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Unsupported address action.");
                return;
            }

            DeliveryAddress address = new DeliveryAddress();
            address.setLabel(stringValue(data.get("label")));
            address.setAddress(stringValue(data.get("address")));
            address.setCity(stringValue(data.get("city")));
            address.setState(stringValue(data.get("state")));
            address.setPincode(stringValue(data.get("pincode")));
            address.setLandmark(stringValue(data.get("landmark")));
            address.setLatitude(doubleValue(data.get("latitude")));
            address.setLongitude(doubleValue(data.get("longitude")));
            address.setAccuracy(doubleValue(data.get("accuracy")));

            String validationError = validate(address);
            if (validationError != null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, validationError);
                return;
            }

            DeliveryAddress created = addressDAO.create(user.getId(), address);
            Map<String, Object> result = success("Delivery address saved.");
            result.put("address", created);
            sendJson(response, HttpServletResponse.SC_CREATED, result);
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid address data.");
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not save the address.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = getLoggedInUser(request);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to remove a saved address.");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "A valid address ID is required.");
            return;
        }

        try {
            if (!addressDAO.delete(user.getId(), id)) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Saved address not found.");
                return;
            }
            sendJson(response, HttpServletResponse.SC_OK, success("Saved address removed."));
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not remove the address.");
        }
    }

    private String validate(DeliveryAddress address) {
        if (address.getLabel() == null || address.getLabel().isBlank() || address.getLabel().length() > 80) {
            return "Spot name is required and must be at most 80 characters.";
        }
        if (address.getAddress() == null || address.getAddress().isBlank() || address.getAddress().length() > 500) {
            return "Full address is required and must be at most 500 characters.";
        }
        if (lengthOver(address.getCity(), 100) || lengthOver(address.getState(), 100)
                || lengthOver(address.getLandmark(), 150)) {
            return "Please shorten the city, state, or landmark field.";
        }
        String pincode = address.getPincode();
        if (pincode != null && !pincode.isBlank() && !pincode.matches("[0-9]{6}")) {
            return "PIN code must contain exactly 6 digits.";
        }
        Double latitude = address.getLatitude();
        Double longitude = address.getLongitude();
        if ((latitude == null) != (longitude == null)
                || (latitude != null && (latitude < -90 || latitude > 90))
                || (longitude != null && (longitude < -180 || longitude > 180))
                || (address.getAccuracy() != null && address.getAccuracy() < 0)) {
            return "Location coordinates are invalid.";
        }
        return null;
    }

    private boolean lengthOver(String value, int max) {
        return value != null && value.length() > max;
    }

    private User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        return value instanceof User ? (User) value : null;
    }

    private Map<String, Object> readJson(HttpServletRequest request) throws IOException {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
        }
        Map<String, Object> data = gson.fromJson(json.toString(), Map.class);
        return data == null ? new HashMap<>() : data;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private int integerValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(stringValue(value)); }
        catch (NumberFormatException e) { return -1; }
    }

    private Double doubleValue(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try { return Double.valueOf(value.toString()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid coordinate", e); }
    }

    private Map<String, Object> success(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        sendJson(response, status, result);
    }

    private void sendJson(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(value));
    }
}
