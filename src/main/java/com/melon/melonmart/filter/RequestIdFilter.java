package com.melon.melonmart.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@WebFilter("/api/*")
public class RequestIdFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String id = UUID.randomUUID().toString();
        ((HttpServletResponse) response).setHeader("X-Request-ID", id);
        request.setAttribute("requestId", id);
        chain.doFilter(request, response);
    }
}
