package com.paul.csvpipeline.backend.lambda;

import com.paul.csvpipeline.backend.IntegrationTestBase;
import com.paul.csvpipeline.backend.IntegrationTestBase.LocalStackS3ClientConfig;
import com.paul.csvpipeline.backend.lambda.handler.CsvValidationLambda;
import com.paul.csvpipeline.backend.lambda.validation.CsvValidator;
import com.paul.csvpipeline.backend.lambda.validation.ExpectedSchema;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Testcontainers;

import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import(LocalStackS3ClientConfig.class)
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CsvValidationLambdaIT extends IntegrationTestBase {

    private final S3Client s3Client;
    private final CsvValidationLambda lambda;

    @Value("${csvpipeline.s3.bucket}")
    private String bucket;

    CsvValidationLambdaIT(S3Client s3Client) {
        this.s3Client = s3Client;
        this.lambda = new CsvValidationLambda(s3Client, new CsvValidator(ExpectedSchema.defaultSchema()));
    }

    @BeforeEach
    void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception ex) {
            createBucket();
        }
    }

    @Test
    void validCsvReturnsSuccess() {
        long uploadId = 10L;
        String key = buildKey(uploadId, "valid.csv");
        putCsv(key, "id,name,email,amount\n1,Jane,jane@example.com,10.5\n");

        var response = lambda.handleRequest(new CsvValidationLambda.ValidationRequest(uploadId, bucket, key), null);

        assertThat(response.valid()).isTrue();
        assertThat(response.errorCount()).isZero();
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void invalidCsvCapturesDetails() {
        long uploadId = 11L;
        String key = buildKey(uploadId, "invalid.csv");
        putCsv(key, "id,name,email,amount\nabc,Jane,bad-email,xx\n");

        var response = lambda.handleRequest(new CsvValidationLambda.ValidationRequest(uploadId, bucket, key), null);

        assertThat(response.valid()).isFalse();
        assertThat(response.errorCount()).isGreaterThanOrEqualTo(2);
        assertThat(response.errors())
                .extracting("column")
                .contains("id", "email", "amount");
    }

    private void putCsv(String key, String content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("text/csv")
                        .build(),
                RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8))
        );
    }

    private String buildKey(long uploadId, String filename) {
        return "uploads/user@example.com/%d/%s".formatted(uploadId, filename);
    }

    private void createBucket() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception ex) {
            if (ex.statusCode() != 409) {
                throw ex;
            }
        }

        s3Client.waiter().waitUntilBucketExists(
                (HeadBucketRequest.Builder b) -> b.bucket(bucket),
                w -> w.maxAttempts(5).backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1)))
        );
    }
}
