package com.paul.csvpipeline.backend.lambda.validation;

public record ValidationError(int rowNumber, String column, String message) {
}
