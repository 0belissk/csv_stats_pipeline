package com.paul.csvpipeline.backend.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.paul.csvpipeline.backend.lambda.parser.S3KeyParser;
import com.paul.csvpipeline.backend.lambda.persistence.UploadStatusRepository;
import com.paul.csvpipeline.backend.lambda.validation.CsvValidationResult;
import com.paul.csvpipeline.backend.lambda.validation.CsvValidator;
import com.paul.csvpipeline.backend.lambda.validation.ExpectedSchema;
import com.paul.csvpipeline.backend.lambda.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.List;

public class CsvValidationLambda implements RequestHandler<S3Event, Void> {

    private static final Logger log = LoggerFactory.getLogger(CsvValidationLambda.class);

    private final S3Client s3Client;
    private final UploadStatusRepository repository;
    private final CsvValidator validator;

    public CsvValidationLambda() {
        this(S3Client.builder().build(), UploadStatusRepository.fromEnv(), new CsvValidator(ExpectedSchema.defaultSchema()));
    }

    public CsvValidationLambda(S3Client s3Client,
                               UploadStatusRepository repository,
                               CsvValidator validator) {
        this.s3Client = s3Client;
        this.repository = repository;
        this.validator = validator;
    }

    @Override
    public Void handleRequest(S3Event event, Context context) {
        if (event == null || event.getRecords() == null) {
            log.warn("Received empty S3 event");
            return null;
        }

        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            handleRecord(record);
        }
        return null;
    }

    private void handleRecord(S3EventNotification.S3EventNotificationRecord record) {
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();

        S3KeyParser.ParsedKey parsedKey = S3KeyParser.parse(key);
        log.info("Starting validation for uploadId={} (bucket={}, key={})", parsedKey.uploadId(), bucket, key);
        repository.markValidating(parsedKey.uploadId());

        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build())) {

            CsvValidationResult result = validator.validate(stream);
            if (result.valid()) {
                repository.markValidated(parsedKey.uploadId());
                log.info("Upload {} validated successfully", parsedKey.uploadId());
            } else {
                repository.markFailed(parsedKey.uploadId(), result.errors());
                log.warn("Upload {} failed validation with {} errors", parsedKey.uploadId(), result.errors().size());
            }
        } catch (IOException ex) {
            log.error("Failed to read S3 object {} from bucket {}", key, bucket, ex);
            repository.markFailed(parsedKey.uploadId(), List.of(
                    new ValidationError(0, "file", "Unable to read object: " + ex.getMessage())
            ));
        }
    }
}
