package com.paul.csvpipeline.backend.csvupload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paul.csvpipeline.backend.IntegrationTestBase;
import com.paul.csvpipeline.backend.IntegrationTestBase.LocalStackS3ClientConfig;
import com.paul.csvpipeline.backend.csvupload.repository.CsvUploadRepository;

import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Import(LocalStackS3ClientConfig.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvUploadControllerIT extends IntegrationTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private CsvUploadRepository uploadRepository;

    @Value("${csvpipeline.s3.bucket}")
    private String bucket;

    @BeforeAll
    void ensureBucket() {
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(60))
                .pollInterval(java.time.Duration.ofSeconds(1))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                    s3Client.headBucket(b -> b.bucket(bucket));
                });
    }

    @BeforeEach
    void cleanState() {
        uploadRepository.deleteAll();
        deleteAllObjectsInBucket();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void uploadCsvStoresInS3AndReturnsPending() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.csv", "text/csv", "id,name\n1,test".getBytes());

        var mvcResult = mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.filename").value("data.csv"))
                .andReturn();

        JsonNode root = MAPPER.readTree(mvcResult.getResponse().getContentAsByteArray());
        long id = root.get("id").asLong();

        String expectedKey = "uploads/user@example.com/%s/data.csv".formatted(id);
        assertThatCode(() -> s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(expectedKey)
                .build()))
                .doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void listAndGetAreUserScoped() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.csv", "text/csv", "id,name\n1,test".getBytes());

        var uploadResult = mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isOk())
                .andReturn();
        long id = MAPPER.readTree(uploadResult.getResponse().getContentAsByteArray()).get("id").asLong();

        mockMvc.perform(get("/api/uploads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/uploads/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void uploadWithoutAuthReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.csv", "text/csv", "id,name\n1,test".getBytes());

        mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isUnauthorized());
    }

    private void deleteAllObjectsInBucket() {
        var listed = s3Client.listObjectsV2(b -> b.bucket(bucket));
        if (listed.contents() == null || listed.contents().isEmpty()) {
            return;
        }

        var ids = listed.contents().stream()
                .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                .collect(Collectors.toList());

        if (ids.size() == 1) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(ids.get(0).key()).build());
            return;
        }

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(ids).build())
                .build());
    }
}