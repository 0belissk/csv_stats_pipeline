package com.paul.csvpipeline.backend.lambda.validation;

public record ColumnDefinition(String name, ColumnType type) {
    public ColumnDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Column type is required");
        }
    }
}
