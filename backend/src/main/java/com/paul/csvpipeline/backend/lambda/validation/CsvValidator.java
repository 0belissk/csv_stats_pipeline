package com.paul.csvpipeline.backend.lambda.validation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CsvValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final List<ColumnDefinition> schema;

    public CsvValidator(List<ColumnDefinition> schema) {
        this.schema = List.copyOf(schema);
    }

    public CsvValidationResult validate(InputStream inputStream) {
        List<ValidationError> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .withAllowMissingColumnNames(false))) {

            validateHeaders(parser, errors);
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                validateRow(record, rowNumber, errors);
                rowNumber++;
            }
        } catch (IOException e) {
            errors.add(new ValidationError(0, "file", "Unable to read CSV: " + e.getMessage()));
        }

        if (errors.isEmpty()) {
            return CsvValidationResult.success();
        }
        return CsvValidationResult.failure(errors);
    }

    private void validateHeaders(CSVParser parser, List<ValidationError> errors) {
        List<String> headers = parser.getHeaderNames();
        if (headers.size() != schema.size()) {
            errors.add(new ValidationError(0, "header", "Expected " + schema.size() + " columns but found " + headers.size()));
            return;
        }

        for (int i = 0; i < schema.size(); i++) {
            String expected = schema.get(i).name().toLowerCase(Locale.ROOT);
            String actual = headers.get(i).toLowerCase(Locale.ROOT);
            if (!expected.equals(actual)) {
                errors.add(new ValidationError(0, "header", "Expected column '" + schema.get(i).name() + "' but found '" + headers.get(i) + "'"));
            }
        }
    }

    private void validateRow(CSVRecord record, int rowNumber, List<ValidationError> errors) {
        for (int i = 0; i < schema.size(); i++) {
            ColumnDefinition column = schema.get(i);
            String value = record.get(i);
            if (value == null || value.isBlank()) {
                errors.add(new ValidationError(rowNumber, column.name(), "Value is required"));
                continue;
            }

            switch (column.type()) {
                case STRING -> {}
                case INTEGER -> validateInteger(value, rowNumber, column.name(), errors);
                case DECIMAL -> validateDecimal(value, rowNumber, column.name(), errors);
                case EMAIL -> validateEmail(value, rowNumber, column.name(), errors);
                case DATE -> validateDate(value, rowNumber, column.name(), errors);
                default -> throw new IllegalStateException("Unsupported column type: " + column.type());
            }
        }
    }

    private void validateInteger(String value, int row, String column, List<ValidationError> errors) {
        try {
            Long.parseLong(value);
        } catch (NumberFormatException ex) {
            errors.add(new ValidationError(row, column, "Value is not an integer"));
        }
    }

    private void validateDecimal(String value, int row, String column, List<ValidationError> errors) {
        try {
            new BigDecimal(value);
        } catch (NumberFormatException ex) {
            errors.add(new ValidationError(row, column, "Value is not numeric"));
        }
    }

    private void validateEmail(String value, int row, String column, List<ValidationError> errors) {
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            errors.add(new ValidationError(row, column, "Value is not a valid email"));
        }
    }

    private void validateDate(String value, int row, String column, List<ValidationError> errors) {
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            errors.add(new ValidationError(row, column, "Invalid date (expected yyyy-MM-dd)"));
        }
    }
}
