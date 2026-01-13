package com.paul.csvpipeline.backend.lambda.parser;

public final class S3KeyParser {

    public record ParsedKey(String userEmail, long uploadId, String filename) {}

    private S3KeyParser() {
    }

    public static ParsedKey parse(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key cannot be empty");
        }

        String[] parts = key.split("/");
        if (parts.length < 4) {
            throw new IllegalArgumentException("S3 key must follow uploads/{userEmail}/{uploadId}/{filename}");
        }

        if (!"uploads".equals(parts[0])) {
            throw new IllegalArgumentException("Unexpected root folder in key: " + parts[0]);
        }

        long uploadId;
        try {
            uploadId = Long.parseLong(parts[2]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Upload id must be numeric in key: " + key, ex);
        }

        String filename = parts[parts.length - 1];
        return new ParsedKey(parts[1], uploadId, filename);
    }
}
