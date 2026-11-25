package com.quizapp.model;

import com.quizapp.validation.ValidationRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private int failedLoginAttempts;

    @Column(nullable = false)
    private boolean locked;

    protected User() {
        // required by JPA
    }

    public User(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /** R5: counts a wrong password and locks the account when the limit is reached. */
    public void recordFailedLogin() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= ValidationRules.FAILED_LOGINS_BEFORE_LOCK) {
            locked = true;
        }
    }

    /** R4: a successful login clears the failed-login counter. */
    public void resetFailedLogins() {
        failedLoginAttempts = 0;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public boolean isLocked() {
        return locked;
    }
}
