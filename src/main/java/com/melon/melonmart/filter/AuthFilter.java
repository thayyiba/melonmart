package com.melon.melonmart.filter;

import com.melon.melonmart.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {"/seller.html", "/admin.html"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("MelonMart AuthFilter initialized.");
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        HttpSession session =
                httpRequest.getSession(false);

        User user = null;

        if (session != null) {
            Object sessionUser =
                    session.getAttribute("user");

            if (sessionUser instanceof User) {
                user = (User) sessionUser;
            }
        }

        // Not logged in
        if (user == null) {

            httpResponse.sendRedirect(
                    httpRequest.getContextPath()
                            + "/login.html"
            );

            return;
        }

        String page = httpRequest.getServletPath();
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();

        if ("/admin.html".equals(page) && !"ADMIN".equals(role)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required.");
            return;
        }

        if ("/seller.html".equals(page) && !"SELLER".equals(role)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Seller access required.");
            return;
        }

        // Seller is authenticated
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        System.out.println("MelonMart AuthFilter destroyed.");
    }
}