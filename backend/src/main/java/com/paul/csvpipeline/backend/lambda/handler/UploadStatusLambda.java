package com.paul.csvpipeline.backend.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.paul.csvpipeline.backend.lambda.persistence.UploadStatusRepository;
import com.paul.csvpipeline.backend.lambda.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Lambda invoked by Step Functions to persist status transitions for CSV uploads.
 */
public class UploadStatusLambda implements RequestHandler<UploadStatusLambda.StatusRequest, UploadStatusLambda.StatusResponse> {

    private static final Logger log = LoggerFactory.getLogger(UploadStatusLambda.class);

    private final UploadStatusRepository repository;

    public UploadStatusLambda() {
        this(UploadStatusRepository.fromEnv());
    }

    public UploadStatusLambda(UploadStatusRepository repository) {
        this.repository = repository;
    }

    @Override
    public StatusResponse handleRequest(StatusRequest input, Context context) {
        if (input == null) {
            throw new IllegalArgumentException("Status request is required");
        }
        long uploadId = input.uploadId();
        String status = input.status();
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status value is required");
        }

        log.info("Updating upload {} status to {}", uploadId, status);
        switch (status) {
            case "VALIDATING" -> repository.markValidating(uploadId);
            case "VALIDATED" -> repository.markValidated(uploadId);
            case "VALIDATION_FAILED" -> repository.markFailed(uploadId, safeErrors(input.errors()));
            default -> throw new IllegalArgumentException("Unsupported status transition: " + status);
        }
        return new StatusResponse(status);
    }

    private List<ValidationError> safeErrors(List<ValidationError> errors) {
        if (errors == null) {
            return Collections.emptyList();
        }
        return errors;
    }

    public record StatusRequest(long uploadId, String status, List<ValidationError> errors) {
    }

    public record StatusResponse(String status) {
    }
}
