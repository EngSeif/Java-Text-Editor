// Source code is decompiled from a .class file using FernFlower decompiler.
package com.editor.backend.controller;

import com.editor.backend.model.Comment;
import com.editor.backend.model.DocumentSession;
import com.editor.backend.service.CRDTService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/documents"})
public class DocumentRestController {
    private static final Map<String, DocumentSession> documentSessions = new ConcurrentHashMap();

    public DocumentRestController() {
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        Map<String, String> error = new HashMap();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(500).body(error);
    }

    @PostMapping({""})
    public ResponseEntity<Map<String, String>> createNewDocument() {
        DocumentSession session = new DocumentSession();
        documentSessions.put(session.getDocId(), session);
        Map<String, String> response = new HashMap();
        response.put("documentId", session.getDocId());
        response.put("editorCode", session.getEditorCode());
        response.put("viewerCode", session.getViewerCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping(
            path = {"/upload/"},
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<Map<String, String>> importFile(@RequestParam("file") MultipartFile file, @RequestParam("documentId") String documentId, @RequestParam("userId") String userId) {
        if (file != null && !file.isEmpty()) {
            if (!file.getContentType().equals("text/plain")) {
                throw new IllegalArgumentException("File Type is not text");
            } else {
                DocumentSession session = (DocumentSession)documentSessions.get(documentId);
                if (session == null) {
                    throw new IllegalArgumentException("Invalid documentId");
                } else {
                    String fileContent;
                    try {
                        fileContent = new String(file.getBytes());
                    } catch (Exception var13) {
                        throw new RuntimeException("Error Reading File");
                    }

                    CRDTService docCRDT = session.getDocCRDT();
                    long clock = System.currentTimeMillis();
                    char[] var9 = fileContent.toCharArray();
                    int var10 = var9.length;

                    for(int var11 = 0; var11 < var10; ++var11) {
                        char c = var9[var11];
                        docCRDT.insertAtCursor(c, userId, clock++);
                    }

                    Map<String, String> response = new HashMap();
                    response.put("message", "uploaded successfully");
                    return ResponseEntity.ok(response);
                }
            }
        } else {
            throw new IllegalArgumentException("file is missing or empty");
        }
    }

    @GetMapping({"/{documentId}"})
    public ResponseEntity<Map<String, String>> getDocument(@PathVariable String documentId) {
        DocumentSession session = (DocumentSession)documentSessions.get(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            String content = session.getDocCRDT().getDocument();
            Map<String, String> response = new HashMap();
            response.put("document", content);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping({"/{documentId}/undo"})
    public ResponseEntity<?> undo(@PathVariable String documentId, @RequestParam("userId") String userId) {
        DocumentSession session = (DocumentSession)documentSessions.get(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().undo(userId);
            return ResponseEntity.ok(Map.of("message", "Undo successful"));
        }
    }

    @PostMapping({"/{documentId}/redo"})
    public ResponseEntity<?> redo(@PathVariable String documentId, @RequestParam("userId") String userId) {
        DocumentSession session = (DocumentSession)documentSessions.get(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().redo(userId);
            return ResponseEntity.ok(Map.of("message", "Redo successful"));
        }
    }

    @PostMapping({"/{documentId}/cursor"})
    public ResponseEntity<?> updateCursor(@PathVariable String documentId, @RequestParam String userId, @RequestParam int index) {
        DocumentSession session = (DocumentSession)documentSessions.get(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().updateCursorByIndex(userId, index);
            return ResponseEntity.ok(Map.of("message", "Cursor updated"));
        }
    }

    @PostMapping({"/{documentId}/comment"})
    public ResponseEntity<?> addComment(@PathVariable String documentId, @RequestParam String userId, @RequestParam String content, @RequestParam int startIndex, @RequestParam int endIndex) {
        DocumentSession session = (DocumentSession)documentSessions.get(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().addCommentFromIndexRange(userId, content, startIndex, endIndex);
            return ResponseEntity.ok(Map.of("message", "Comment added"));
        }
    }

    @PostMapping({"/{documentId}/comment/delete"})
    public ResponseEntity<?> deleteComment(@PathVariable String documentId, @RequestParam String commentId) {
        DocumentSession session = (DocumentSession)documentSessions.get(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().deleteComment(commentId);
            return ResponseEntity.ok(Map.of("message", "Comment deleted"));
        }
    }

    @GetMapping({"/{documentId}/comments"})
    public ResponseEntity<?> getAllComments(@PathVariable String documentId) {
        DocumentSession session = (DocumentSession)documentSessions.get(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            List<Comment> comments = session.getDocCRDT().getAllComments();
            return ResponseEntity.ok(comments);
        }
    }

    @GetMapping({"/generateUserId"})
    public ResponseEntity<Map<String, String>> generateUserId() {
        String userId = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of("userId", userId));
    }
}