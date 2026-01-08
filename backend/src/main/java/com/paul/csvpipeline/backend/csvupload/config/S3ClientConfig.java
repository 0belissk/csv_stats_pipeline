package com.paul.csvpipeline.backend.csvupload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
class S3ClientConfig {

    @Value("${csvpipeline.aws.region:us-east-1}")
    private String region;

    // Dynamic in tests via LocalStackS3TestContainer; empty in prod
    @Value("${csvpipeline.aws.endpoint:}")
    private String endpointOverride;

    @Bean
    S3Client s3Client() {
        // Use LocalStack default creds (“test”/“test”) for dev/test
        var creds = AwsBasicCredentials.create("test", "test");

        software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .overrideConfiguration(ClientOverrideConfiguration.builder().build());

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }
}