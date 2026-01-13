package com.paul.csvpipeline.backend.lambda.persistence;

import com.paul.csvpipeline.backend.lambda.validation.ValidationError;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UploadStatusRepository {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public UploadStatusRepository(String jdbcUrl, String username, String password) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl is required");
        this.username = Objects.requireNonNull(username, "username is required");
        this.password = Objects.requireNonNull(password, "password is required");
    }

    public static UploadStatusRepository fromEnv() {
        return new UploadStatusRepository(
                requireEnv("DB_URL"),
                requireEnv("DB_USERNAME"),
                requireEnv("DB_PASSWORD")
        );
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    public void markValidating(long uploadId) {
        update(uploadId, "VALIDATING", null);
    }

    public void markValidated(long uploadId) {
        update(uploadId, "VALIDATED", null);
    }

    public void markFailed(long uploadId, List<ValidationError> errors) {
        update(uploadId, "VALIDATION_FAILED", buildErrorPayload(errors));
    }

    private void update(long uploadId, String status, String errorPayload) {
        String sql = "UPDATE csv_uploads SET status = ?, error_message = ?, updated_at = NOW() WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, errorPayload);
            statement.setLong(3, uploadId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update upload status for id=" + uploadId, e);
        }
    }

    private String buildErrorPayload(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        return errors.stream()
                .limit(25)
                .map(err -> "{\"row\":" + err.rowNumber() +
                        ",\"column\":\"" + escape(err.column()) +
                        "\",\"message\":\"" + escape(err.message()) + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
