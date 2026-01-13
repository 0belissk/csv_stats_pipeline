package com.paul.csvpipeline.backend.lambda.validation;

import java.util.List;

public record CsvValidationResult(boolean valid, List<ValidationError> errors) {
    public static CsvValidationResult success() {
        return new CsvValidationResult(true, List.of());
    }

    public static CsvValidationResult failure(List<ValidationError> errors) {
        return new CsvValidationResult(false, errors);
    }
}
