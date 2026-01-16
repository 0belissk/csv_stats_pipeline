package com.paul.csvpipeline.backend.auth.dto;

public record AuthResponse(
        Long userId,
        String email,
        String token
) {}