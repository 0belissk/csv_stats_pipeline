package com.paul.csvpipeline.backend.csvupload;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class UploadMetrics {

    private final Counter uploadRequested;
    private final Counter uploadAccepted;
    private final Counter uploadRejected;
    private final Counter statusPolled;

    public UploadMetrics(MeterRegistry registry) {
        this.uploadRequested = Counter.builder("csv.upload.requests")
                .description("Number of CSV upload attempts")
                .register(registry);
        this.uploadAccepted = Counter.builder("csv.upload.accepted")
                .description("Number of CSV uploads accepted for processing")
                .register(registry);
        this.uploadRejected = Counter.builder("csv.upload.rejected")
                .description("Number of CSV uploads rejected due to validation")
                .register(registry);
        this.statusPolled = Counter.builder("csv.upload.status.polled")
                .description("Number of times upload status was polled")
                .register(registry);
    }

    public void markRequested() {
        uploadRequested.increment();
    }

    public void markAccepted() {
        uploadAccepted.increment();
    }

    public void markRejected() {
        uploadRejected.increment();
    }

    public void markStatusPolled() {
        statusPolled.increment();
    }
}