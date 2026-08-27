package com.taskmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test verifying OpenAPI 3.1 specification generation and exporting to docs/api/openapi.yaml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Export and validate OpenAPI 3.1 specification to docs/api/openapi.yaml")
    void exportOpenApiSpec() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs.yaml"))
            .andExpect(status().isOk())
            .andReturn();

        String yamlContent = result.getResponse().getContentAsString();
        assertThat(yamlContent).isNotBlank();
        assertThat(yamlContent).contains("openapi: 3.");
        assertThat(yamlContent).contains("TaskMaster");

        Path targetDir = Paths.get("docs/api");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetFile = targetDir.resolve("openapi.yaml");
        Files.writeString(targetFile, yamlContent);

        File exportedFile = targetFile.toFile();
        assertThat(exportedFile).exists();
        assertThat(exportedFile.length()).isGreaterThan(1000);
    }
}
