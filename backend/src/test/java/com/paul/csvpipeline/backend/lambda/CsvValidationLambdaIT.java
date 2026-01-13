// backend/src/test/java/com/paul/csvpipeline/backend/lambda/CsvValidationLambdaIT.java
package com.paul.csvpipeline.backend.lambda;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.paul.csvpipeline.backend.LocalStackS3TestContainer;
import com.paul.csvpipeline.backend.csvupload.CsvUploadStatus;
import com.paul.csvpipeline.backend.csvupload.entity.CsvUpload;
import com.paul.csvpipeline.backend.csvupload.repository.CsvUploadRepository;
import com.paul.csvpipeline.backend.lambda.handler.CsvValidationLambda;
import com.paul.csvpipeline.backend.lambda.persistence.UploadStatusRepository;
import com.paul.csvpipeline.backend.lambda.validation.CsvValidator;
import com.paul.csvpipeline.backend.lambda.validation.ExpectedSchema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(LocalStackS3TestContainer.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CsvValidationLambdaIT {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("csvpipeline")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start(); // ensures container is up before Spring reads properties
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CsvUploadRepository repository;

    @Autowired
    private S3Client s3Client;

    @Value("${csvpipeline.s3.bucket}")
    private String bucket;

    private UploadStatusRepository uploadStatusRepository;

    @BeforeAll
    void initRepo() {
        // use container values directly; do not depend on Environment properties
        this.uploadStatusRepository = new UploadStatusRepository(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        ensureBucketExists();
    }

    @BeforeEach
    void clean() {
        repository.deleteAll();
        ensureBucketExists();
    }

    @Test
    void validCsvUpdatesStatus() {
        CsvUpload upload = seedUpload("user@example.com", "valid.csv");
        putCsv(upload.getS3Key(), "id,name,email,amount\n1,Jane,jane@example.com,10.5\n");

        CsvValidationLambda lambda = new CsvValidationLambda(
                s3Client,
                uploadStatusRepository,
                new CsvValidator(ExpectedSchema.defaultSchema())
        );

        lambda.handleRequest(buildS3Event(upload.getS3Key()), null);

        CsvUpload refreshed = repository.findById(upload.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(CsvUploadStatus.VALIDATED);
        assertThat(refreshed.getErrorMessage()).isNull();
    }

    @Test
    void invalidCsvCapturesErrors() {
        CsvUpload upload = seedUpload("user@example.com", "invalid.csv");
        putCsv(upload.getS3Key(), "id,name,email,amount\nabc,Jane,bad-email,zz\n");

        CsvValidationLambda lambda = new CsvValidationLambda(
                s3Client,
                uploadStatusRepository,
                new CsvValidator(ExpectedSchema.defaultSchema())
        );

        lambda.handleRequest(buildS3Event(upload.getS3Key()), null);

        CsvUpload refreshed = repository.findById(upload.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(CsvUploadStatus.VALIDATION_FAILED);
        assertThat(refreshed.getErrorMessage()).contains("id").contains("amount").contains("email");
    }

    private CsvUpload seedUpload(String email, String filename) {
        CsvUpload upload = new CsvUpload(email, filename, 100, "text/csv", "");
        CsvUpload saved = repository.save(upload);
        String key = "uploads/%s/%s/%s".formatted(email, saved.getId(), filename);
        saved.setS3Key(key);
        return repository.save(saved);
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

    private S3Event buildS3Event(String key) {
        S3EventNotification.S3BucketEntity bucketEntity = new S3EventNotification.S3BucketEntity(
                bucket,
                new S3EventNotification.UserIdentityEntity("AIDTEST"),
                null
        );
        S3EventNotification.S3ObjectEntity objectEntity = new S3EventNotification.S3ObjectEntity(
                key,
                0L,
                null,
                null,
                null
        );
        S3EventNotification.S3Entity s3Entity = new S3EventNotification.S3Entity(
                null,
                bucketEntity,
                objectEntity,
                null
        );
        S3EventNotification.S3EventNotificationRecord record = new S3EventNotification.S3EventNotificationRecord(
                "us-east-1",
                "ObjectCreated:Put",
                "aws:s3",
                "2020-01-01T00:00:00.000Z",
                "2.1",
                null,
                null,
                s3Entity,
                new S3EventNotification.UserIdentityEntity("AIDTEST")
        );
        return new S3Event(List.of(record));
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception ex) {
            createBucket();
        }
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
