package com.paul.csvpipeline.backend.lambda.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3KeyParserTest {

    @Test
    void parseValidKey() {
        S3KeyParser.ParsedKey parsed = S3KeyParser.parse("uploads/user@example.com/42/data.csv");
        assertThat(parsed.userEmail()).isEqualTo("user@example.com");
        assertThat(parsed.uploadId()).isEqualTo(42L);
        assertThat(parsed.filename()).isEqualTo("data.csv");
    }

    @Test
    void parseInvalidKeyThrows() {
        assertThatThrownBy(() -> S3KeyParser.parse("bad/key"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
