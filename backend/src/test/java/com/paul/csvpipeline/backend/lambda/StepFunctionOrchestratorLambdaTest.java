package com.paul.csvpipeline.backend.lambda;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paul.csvpipeline.backend.lambda.handler.StepFunctionOrchestratorLambda;
import com.paul.csvpipeline.backend.lambda.persistence.UploadStatusRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StepFunctionOrchestratorLambdaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startsStateMachineAndMarksStatus() throws Exception {
        SfnClient sfnClient = mock(SfnClient.class);
        when(sfnClient.startExecution(any(StartExecutionRequest.class)))
                .thenReturn(StartExecutionResponse.builder().executionArn("arn").build());

        UploadStatusRepository repository = mock(UploadStatusRepository.class);
        StepFunctionOrchestratorLambda lambda = new StepFunctionOrchestratorLambda(
                sfnClient,
                repository,
                "arn:aws:states:us-east-1:123:stateMachine:csv-pipeline",
                objectMapper
        );

        S3Event event = new S3Event(List.of(buildRecord("csv-upload-bucket", "uploads/user@example.com/123/data.csv")));

        lambda.handleRequest(event, null);

        verify(repository).markValidating(123L);
        ArgumentCaptor<StartExecutionRequest> requestCaptor = ArgumentCaptor.forClass(StartExecutionRequest.class);
        verify(sfnClient).startExecution(requestCaptor.capture());

        StepFunctionOrchestratorLambda.StateMachineInput input = objectMapper.readValue(
                requestCaptor.getValue().input(),
                StepFunctionOrchestratorLambda.StateMachineInput.class
        );

        assertThat(input.uploadId()).isEqualTo(123L);
        assertThat(input.bucket()).isEqualTo("csv-upload-bucket");
        assertThat(input.key()).isEqualTo("uploads/user@example.com/123/data.csv");
    }

    private S3EventNotification.S3EventNotificationRecord buildRecord(String bucket, String key) {
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
        return new S3EventNotification.S3EventNotificationRecord(
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
    }
}
