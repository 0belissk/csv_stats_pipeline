// backend/src/test/java/com/paul/csvpipeline/backend/IntegrationTestBase.java
package com.paul.csvpipeline.backend;

import java.net.URI;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Base class to share Testcontainers across ALL integration tests in the same JVM.
 *
 * Key rules:
 * - One Postgres + one LocalStack for the full IT run (single fork).
 * - Do NOT use @DirtiesContext(AFTER_EACH_TEST_METHOD) in ITs; clean state instead.
 *
 * Implementation detail:
 * - We do NOT use the JUnit Testcontainers extension (@Testcontainers/@Container),
 *   because it stops containers after each test class. We start them once per JVM here.
 */
public abstract class IntegrationTestBase {

    private static final Object START_LOCK = new Object();
    protected static final String TEST_BUCKET = "csvpipeline-test-uploads";

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("csvpipeline")
                    .withUsername("csvpipeline")
                    .withPassword("csvpipeline");

    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.3"))
                    .withServices(LocalStackContainer.Service.S3);

    private static void ensureContainersStarted() {
        if (POSTGRES.isRunning() && LOCALSTACK.isRunning()) {
            return;
        }
        synchronized (START_LOCK) {
            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }
            if (!LOCALSTACK.isRunning()) {
                LOCALSTACK.start();
            }
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        ensureContainersStarted();

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");

        registry.add("csvpipeline.aws.region", LOCALSTACK::getRegion);
        registry.add("csvpipeline.s3.bucket", () -> TEST_BUCKET);
    }

    @TestConfiguration
    public static class LocalStackS3ClientConfig {
        @Bean
        @Primary
        public S3Client testS3Client() {
            ensureContainersStarted();

            URI endpoint = LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3);

            return S3Client.builder()
                    .region(Region.of(LOCALSTACK.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .endpointOverride(endpoint)
                    .build();
        }
    }
}
