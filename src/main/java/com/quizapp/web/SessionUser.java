package com.quizapp.web;

import com.quizapp.model.Role;
import com.quizapp.model.User;

import java.io.Serializable;

/**
 * The logged-in user as stored in the HTTP session (no password data).
 */
public record SessionUser(Long id, String name, Role role) implements Serializable {

    public static final String SESSION_KEY = "currentUser";

    public static SessionUser from(User user) {
        return new SessionUser(user.getId(), user.getName(), user.getRole());
    }

    /** R4: each role has its own dashboard. */
    public String dashboardPath() {
        return role == Role.INSTRUCTOR ? "/instructor/dashboard" : "/student/dashboard";
    }
}
