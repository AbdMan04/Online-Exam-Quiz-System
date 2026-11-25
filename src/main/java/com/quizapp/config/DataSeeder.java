package com.quizapp.config;

import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the demo instructor account on startup (registration only creates students, see R1).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    static final String INSTRUCTOR_EMAIL = "instructor@uni.edu";
    static final String INSTRUCTOR_PASSWORD = "Teach1234";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(INSTRUCTOR_EMAIL)) {
            userRepository.save(new User("Demo Instructor", INSTRUCTOR_EMAIL,
                    passwordEncoder.encode(INSTRUCTOR_PASSWORD), Role.INSTRUCTOR));
        }
    }
}
