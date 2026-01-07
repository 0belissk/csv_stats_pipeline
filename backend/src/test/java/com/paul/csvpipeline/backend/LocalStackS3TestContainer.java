package com.paul.csvpipeline.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class LocalStackS3TestContainer {

    private static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.3"))
                    .withServices(LocalStackContainer.Service.S3);

    static {
        LOCALSTACK.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("csvpipeline.aws.endpoint",
                () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("csvpipeline.aws.region", LOCALSTACK::getRegion);
        registry.add("csvpipeline.s3.bucket", () -> "csvpipeline-test-uploads");
    }
}