package com.paul.csvpipeline.backend.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paul.csvpipeline.backend.lambda.parser.S3KeyParser;
import com.paul.csvpipeline.backend.lambda.persistence.UploadStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.SfnClientBuilder;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Receives S3 notifications and kicks off the Step Functions pipeline.
 */
public class StepFunctionOrchestratorLambda implements RequestHandler<S3Event, Void> {

    private static final Logger log = LoggerFactory.getLogger(StepFunctionOrchestratorLambda.class);

    private final SfnClient sfnClient;
    private final UploadStatusRepository statusRepository;
    private final String stateMachineArn;
    private final ObjectMapper objectMapper;

    public StepFunctionOrchestratorLambda() {
        this(
                buildDefaultSfnClient(),
                UploadStatusRepository.fromEnv(),
                requireEnv("STATE_MACHINE_ARN"),
                new ObjectMapper()
        );
    }

    public StepFunctionOrchestratorLambda(SfnClient sfnClient,
                                          UploadStatusRepository statusRepository,
                                          String stateMachineArn,
                                          ObjectMapper objectMapper) {
        this.sfnClient = sfnClient;
        this.statusRepository = statusRepository;
        this.stateMachineArn = stateMachineArn;
        this.objectMapper = objectMapper;
    }

    @Override
    public Void handleRequest(S3Event event, Context context) {
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            log.warn("Received empty S3 event, nothing to orchestrate");
            return null;
        }

        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            startExecution(record);
        }
        return null;
    }

    private void startExecution(S3EventNotification.S3EventNotificationRecord record) {
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getUrlDecodedKey();

        S3KeyParser.ParsedKey parsedKey = S3KeyParser.parse(key);
        log.info("Launching state machine for uploadId={} (bucket={}, key={})",
                parsedKey.uploadId(), bucket, key);

        statusRepository.markValidating(parsedKey.uploadId());

        StateMachineInput input = new StateMachineInput(parsedKey.uploadId(), bucket, key);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize Step Functions input", ex);
        }

        StartExecutionRequest request = StartExecutionRequest.builder()
                .stateMachineArn(stateMachineArn)
                .name(buildExecutionName(parsedKey.uploadId()))
                .input(payload)
                .build();

        try {
            sfnClient.startExecution(request);
        } catch (SfnException ex) {
            log.error("Failed to start Step Functions execution for upload {}", parsedKey.uploadId(), ex);
            throw ex;
        }
    }

    private String buildExecutionName(long uploadId) {
        String raw = "upload-%d-%d-%s".formatted(uploadId, Instant.now().toEpochMilli(), UUID.randomUUID());
        String sanitized = raw.replaceAll("[^A-Za-z0-9-]", "");
        return sanitized.length() > 77 ? sanitized.substring(0, 77) : sanitized;
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (isBlank(value)) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    private static SfnClient buildDefaultSfnClient() {
        SfnClientBuilder builder = SfnClient.builder();

        String region = System.getenv("AWS_REGION");
        if (!isBlank(region)) {
            builder = builder.region(Region.of(region));
        }

        String endpoint = System.getenv("SFN_ENDPOINT");
        if (!isBlank(endpoint)) {
            builder = builder.endpointOverride(URI.create(endpoint));
        }

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

    public record StateMachineInput(long uploadId, String bucket, String key) {}
}
