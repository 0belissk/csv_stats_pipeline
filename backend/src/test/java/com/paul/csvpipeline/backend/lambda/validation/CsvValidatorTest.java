package com.paul.csvpipeline.backend.lambda.validation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvValidatorTest {

    private final CsvValidator validator = new CsvValidator(ExpectedSchema.defaultSchema());

    @Test
    void validCsvShouldPass() {
        String csv = "id,name,email,amount\n" +
                "1,Jane Doe,jane@example.com,120.50\n";

        CsvValidationResult result = validator.validate(toStream(csv));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalidHeaderShouldFail() {
        String csv = "identifier,name,email,amount\n" +
                "1,Jane Doe,jane@example.com,120.50\n";

        CsvValidationResult result = validator.validate(toStream(csv));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .anyMatch(error -> error.column().equals("header"));
    }

    @Test
    void invalidRowsShouldReturnDetailedErrors() {
        String csv = "id,name,email,amount\n" +
                "abc,Jane Doe,bad-email,zz\n";

        CsvValidationResult result = validator.validate(toStream(csv));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .extracting(ValidationError::column)
                .contains("id", "email", "amount");
    }

    @Test
    void dateColumnValidatesFormat() {
        List<ColumnDefinition> schema = List.of(
                new ColumnDefinition("id", ColumnType.INTEGER),
                new ColumnDefinition("date", ColumnType.DATE)
        );
        CsvValidator dateValidator = new CsvValidator(schema);

        CsvValidationResult ok = dateValidator.validate(toStream("id,date\n1,2024-01-31\n"));
        assertThat(ok.valid()).isTrue();

        CsvValidationResult bad = dateValidator.validate(toStream("id,date\n1,31/01/2024\n"));
        assertThat(bad.valid()).isFalse();
        assertThat(bad.errors())
                .anyMatch(err -> err.column().equals("date") && err.message().contains("Invalid date"));
    }

    private ByteArrayInputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
