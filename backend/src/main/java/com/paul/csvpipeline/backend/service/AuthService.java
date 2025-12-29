package com.paul.csvpipeline.backend.service;

import com.paul.csvpipeline.backend.domain.User;
import com.paul.csvpipeline.backend.dto.AuthResponse;
import com.paul.csvpipeline.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hash = passwordEncoder.encode(password);
        User user = userRepository.save(new User(email, hash));

        return new AuthResponse(user.getId(), user.getEmail());
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return new AuthResponse(user.getId(), user.getEmail());
    }
}