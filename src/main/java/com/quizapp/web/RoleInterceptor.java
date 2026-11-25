package com.quizapp.web;

import com.quizapp.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Lets a request through only when the session user has the role this interceptor protects.
 * Anyone else is sent to the login page.
 */
public class RoleInterceptor implements HandlerInterceptor {

    private final Role requiredRole;

    public RoleInterceptor(Role requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(SessionUser.SESSION_KEY);
        if (user instanceof SessionUser sessionUser && sessionUser.role() == requiredRole) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
