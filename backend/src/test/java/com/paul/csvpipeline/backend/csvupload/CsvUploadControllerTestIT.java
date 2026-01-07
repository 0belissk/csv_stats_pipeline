package com.paul.csvpipeline.backend.csvupload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paul.csvpipeline.backend.LocalStackS3TestContainer;
import com.paul.csvpipeline.backend.PostgresTestContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, LocalStackS3TestContainer.class})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CsvUploadControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private S3Client s3Client;

    @Value("${csvpipeline.s3.bucket}")
    private String bucket;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void ensureBucket(@Autowired S3Client s3Client,
                             @Value("${csvpipeline.s3.bucket}") String bucket) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
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
}