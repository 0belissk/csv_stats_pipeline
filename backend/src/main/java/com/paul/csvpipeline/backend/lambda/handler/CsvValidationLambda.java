package com.paul.csvpipeline.backend.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.paul.csvpipeline.backend.lambda.validation.CsvValidationResult;
import com.paul.csvpipeline.backend.lambda.validation.CsvValidator;
import com.paul.csvpipeline.backend.lambda.validation.ExpectedSchema;
import com.paul.csvpipeline.backend.lambda.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.List;

/**
 * Lambda handler invoked by Step Functions to validate a CSV object stored in S3.
 */
public class CsvValidationLambda implements RequestHandler<CsvValidationLambda.ValidationRequest, CsvValidationLambda.ValidationResponse> {

    private static final Logger log = LoggerFactory.getLogger(CsvValidationLambda.class);

    private final S3Client s3Client;
    private final CsvValidator validator;

    public CsvValidationLambda() {
        this(buildDefaultS3Client(), new CsvValidator(ExpectedSchema.defaultSchema()));
    }

    public CsvValidationLambda(S3Client s3Client,
                               CsvValidator validator) {
        this.s3Client = s3Client;
        this.validator = validator;
    }

    @Override
    public ValidationResponse handleRequest(ValidationRequest input, Context context) {
        if (input == null) {
            throw new IllegalArgumentException("Validation input is required");
        }
        if (input.bucket() == null || input.bucket().isBlank()) {
            throw new IllegalArgumentException("S3 bucket is required");
        }
        if (input.key() == null || input.key().isBlank()) {
            throw new IllegalArgumentException("S3 object key is required");
        }

        log.info("Running validation for uploadId={} (bucket={}, key={})",
                input.uploadId(), input.bucket(), input.key());

        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(input.bucket())
                .key(input.key())
                .build())) {

            CsvValidationResult result = validator.validate(stream);
            List<ValidationError> trimmedErrors = result.errors() == null ? List.of() : result.errors().stream()
                    .limit(25)
                    .toList();

            if (result.valid()) {
                log.info("Upload {} passed validation", input.uploadId());
            } else {
                log.warn("Upload {} failed validation with {} issues",
                        input.uploadId(), trimmedErrors.size());
            }

            return new ValidationResponse(input.uploadId(), result.valid(), trimmedErrors.size(), trimmedErrors);
        } catch (IOException ex) {
            log.error("Unable to read S3 object {} from bucket {}", input.key(), input.bucket(), ex);
            throw new IllegalStateException("Failed to read CSV from S3", ex);
        }
    }

    /** Input structure provided by the Step Functions state machine. */
    public record ValidationRequest(long uploadId, String bucket, String key) {
    }

    /** Response returned to the state machine for branching and logging. */
    public record ValidationResponse(long uploadId, boolean valid, int errorCount, List<ValidationError> errors) {
    }

    private static S3Client buildDefaultS3Client() {
        S3ClientBuilder builder = S3Client.builder();

        String region = System.getenv("AWS_REGION");
        if (!isBlank(region)) {
            builder = builder.region(Region.of(region));
        }

        String endpoint = System.getenv("S3_ENDPOINT");
        if (!isBlank(endpoint)) {
            builder = builder.endpointOverride(java.net.URI.create(endpoint));
        }

        builder = builder
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClientBuilder(UrlConnectionHttpClient.builder());

        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (!isBlank(accessKey) && !isBlank(secretKey)) {
            builder = builder.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            );
        }

        return builder.build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
