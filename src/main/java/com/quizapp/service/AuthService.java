package com.quizapp.service;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import com.quizapp.validation.AccountValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account and access: R1 register, R2 password rule, R3 duplicate email, R4 login, R5 account lock.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** R1-R3: creates a STUDENT account, or throws with the first rule that fails. */
    @Transactional
    public User register(String name, String email, String password) {
        AccountValidator.validateRegistration(name, email, password);
        String normalizedEmail = AccountValidator.normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(Messages.EMAIL_TAKEN);
        }
        User user = new User(name.trim(), normalizedEmail, passwordEncoder.encode(password), Role.STUDENT);
        return userRepository.save(user);
    }

    /** R4-R5: returns the logged-in user, or throws "Invalid email or password" / "Account locked". */
    @Transactional(noRollbackFor = BusinessException.class)
    public User login(String email, String password) {
        User user = userRepository.findByEmail(AccountValidator.normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(Messages.BAD_CREDENTIALS));
        if (user.isLocked()) {
            throw new BusinessException(Messages.ACCOUNT_LOCKED);
        }
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            throw new BusinessException(user.isLocked() ? Messages.ACCOUNT_LOCKED : Messages.BAD_CREDENTIALS);
        }
        user.resetFailedLogins();
        return userRepository.save(user);
    }
}
