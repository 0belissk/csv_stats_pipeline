package com.paul.csvpipeline.backend.service;

import com.paul.csvpipeline.backend.domain.User;
import com.paul.csvpipeline.backend.dto.AuthResponse;
import com.paul.csvpipeline.backend.repository.UserRepository;
import com.paul.csvpipeline.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hash = passwordEncoder.encode(password);
        User user = userRepository.save(new User(email, hash));

        return new AuthResponse(user.getId(), user.getEmail(), null);
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(user.getId(), user.getEmail(), token);
    }
}