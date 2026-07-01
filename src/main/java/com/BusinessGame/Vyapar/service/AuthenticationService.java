package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.exception.VyaparException;
import com.BusinessGame.Vyapar.dto.LoginRequest;
import com.BusinessGame.Vyapar.dto.LoginResponse;
import com.BusinessGame.Vyapar.entity.User;
import com.BusinessGame.Vyapar.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, 
                                 TokenService tokenService,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            // Verify Password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new VyaparException("Invalid username or password", "INVALID_CREDENTIALS", org.springframework.http.HttpStatus.UNAUTHORIZED);
            }
        } else {
            // Register new user with username and password
            user = new User();
            user.setId(UUID.randomUUID());
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            user = userRepository.save(user);
        }

        String token = tokenService.generateToken(user);
        return new LoginResponse(user.getId(), user.getUsername(), token);
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("No authenticated user found in context");
    }
}
