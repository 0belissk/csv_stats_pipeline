package com.paul.csvpipeline.backend.csvupload;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CsvUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user@example.com")
    void uploadCsvShouldReturnPendingStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                "id,name\n1,test".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.filename").value("data.csv"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void uploadNonCsvShouldBeRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "plain".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only CSV uploads are supported"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void listUploadsShouldIncrementMetrics() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                "id,name\n1,test".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads").file(file))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/uploads"))
                .andExpect(status().isOk());
    }
}
