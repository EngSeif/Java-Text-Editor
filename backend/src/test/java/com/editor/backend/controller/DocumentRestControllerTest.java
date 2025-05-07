package com.editor.backend.controller;

import com.editor.backend.service.CRDTService;
import com.editor.backend.service.DocumentSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentRestController.class)
public class DocumentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CRDTService crdtService;

    @MockBean
    private DocumentSessionService documentSessionService;

    private String documentId;
    private final String userId = "test-user";

    @BeforeEach
    public void setup() throws Exception {
        // Mock creation response
        String response = mockMvc.perform(post("/api/documents"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> result = objectMapper.readValue(response, Map.class);
        documentId = result.get("documentId");
    }

    @Test
    public void testGetDocumentInitiallyEmpty() throws Exception {
        mockMvc.perform(get("/api/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value(""));
    }

    @Test
    public void testUploadPlainTextFile() throws Exception {
        String text = "Hello, CRDT!";
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.txt", "text/plain", text.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/documents/upload/")
                        .file(file)
                        .param("userId", userId)
                        .param("documentId", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("uploaded successfully"));

        mockMvc.perform(get("/api/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value(text));
    }

    @Test
    public void testUndoRedoOperations() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", "text/plain", "A".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/documents/upload/")
                        .file(file)
                        .param("userId", userId)
                        .param("documentId", documentId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/documents/{documentId}/undo", documentId)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Undo successful"));

        mockMvc.perform(get("/api/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value(""));

        mockMvc.perform(post("/api/documents/{documentId}/redo", documentId)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Redo successful"));

        mockMvc.perform(get("/api/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("A"));
    }

    @Test
    public void testUpdateCursor() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "abc.txt", "text/plain", "ABC".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/documents/upload/")
                        .file(file)
                        .param("userId", userId)
                        .param("documentId", documentId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/documents/{documentId}/cursor", documentId)
                        .param("userId", userId)
                        .param("index", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cursor updated"));
    }
}
