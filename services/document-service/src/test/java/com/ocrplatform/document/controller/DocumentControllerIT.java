package com.ocrplatform.document.controller;

import com.ocrplatform.document.AbstractIntegrationTest;
import com.ocrplatform.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration end-to-end du DocumentController.
 * <p>
 * Demarre MySQL + MinIO + Kafka via Testcontainers (cf. {@link AbstractIntegrationTest}).
 * Authentification simulee avec JWT mock (Spring Security Test).
 */
class DocumentControllerIT extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    DocumentRepository documentRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        documentRepository.deleteAll();
    }

    @Test
    void upload_validPdf_returns201AndPersistsDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("test-user-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalName").value("test.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.ownerId").value("test-user-id"));

        assertThat(documentRepository.count()).isEqualTo(1);
    }

    @Test
    void upload_unsupportedFormat_returns422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.zip", "application/zip", "zip content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("test-user-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOC-002"));
    }

    @Test
    void upload_withoutAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "x".getBytes()
        );

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isUnauthorized());
    }
}
