package com.paul.csvpipeline.backend.dto;

public record AuthResponse(
        Long userId,
        String email
) {}