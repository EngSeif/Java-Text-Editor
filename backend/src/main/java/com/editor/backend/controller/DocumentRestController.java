// Source code is decompiled from a .class file using FernFlower decompiler.
package com.editor.backend.controller;

import com.editor.backend.model.Comment;
import com.editor.backend.model.DocumentSession;
import com.editor.backend.model.User;
import com.editor.backend.service.CRDTService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.editor.backend.service.DocumentSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/documents"})
public class DocumentRestController {
    private final DocumentSessionService documentSessionService;

    public DocumentRestController(DocumentSessionService documentSessionService) {
        this.documentSessionService = documentSessionService;
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
        documentSessionService.addDocumentSession(session.getDocId(), session);
        Map<String, String> response = new HashMap();
        User newUser = new User("editor");
        session.addToUsers(newUser);
        response.put("documentId", session.getDocId());
        response.put("editorCode", session.getEditorCode());
        response.put("viewerCode", session.getViewerCode());
        response.put("userId", newUser.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(
            path = {"/upload"},
            consumes = {"application/json"}
    )
    public ResponseEntity<Map<String, String>> importFile(@RequestBody Map<String, String> requestBody) {
        System.out.println("Server received Import Request");
        System.out.println(requestBody);
        String fileContent = requestBody.get("fileContent");

        if (fileContent == null || fileContent.isBlank()) {
            throw new IllegalArgumentException("file content is missing or empty");
        }

        DocumentSession session = new DocumentSession();
        User newUser = new User("editor");
        session.addToUsers(newUser);
        documentSessionService.addDocumentSession(session.getDocId(), session);
        CRDTService docCRDT = session.getDocCRDT();
        long clock = System.currentTimeMillis();

        for (char c : fileContent.toCharArray()) {
            docCRDT.insertAtCursor(c, newUser.getUserId(), clock++);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "uploaded successfully");
        response.put("userID", newUser.getUserId());
        response.put("documentId", session.getDocId());
        response.put("editorCode", session.getEditorCode());
        response.put("viewerCode", session.getViewerCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = {"/userJoin/"})
    public ResponseEntity<Map<String, String>> connectUser(@RequestBody Map<String, String> requestBody) {
        String code = requestBody.get("enteredCode");
        DocumentSession reqSession = documentSessionService.findDocumentSessionByCode(code);

        Map<String, String> response = new HashMap<>();

        if (reqSession == null) {
            response.put("message", "Document session not found for entered code");
            return ResponseEntity.status(404).body(response);  // Return 404 error
        }

        // Determine the user type (editor or viewer)
        User newUser = new User(code.startsWith("E") ? "editor" : "viewer");

        // Add the new user to the document session
        reqSession.addToUsers(newUser);

        // Prepare the response data
        response.put("documentId", reqSession.getDocId());
        response.put("userId", newUser.getUserId());
        response.put("role", code.startsWith("E") ? "editor" : "viewer");

        // Return success response
        return ResponseEntity.ok(response);
    }



    @GetMapping({"/{documentId}"})
    public ResponseEntity<Map<String, String>> getDocument(@PathVariable String documentId) {
        DocumentSession session = documentSessionService.getDocumentSession(documentId);
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
        DocumentSession session = documentSessionService.getDocumentSession(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().undo(userId);
            return ResponseEntity.ok(Map.of("message", "Undo successful"));
        }
    }

    @PostMapping({"/{documentId}/redo"})
    public ResponseEntity<?> redo(@PathVariable String documentId, @RequestParam("userId") String userId) {
        DocumentSession session = documentSessionService.getDocumentSession(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().redo(userId);
            return ResponseEntity.ok(Map.of("message", "Redo successful"));
        }
    }

    @PostMapping({"/{documentId}/cursor"})
    public ResponseEntity<?> updateCursor(@PathVariable String documentId, @RequestParam String userId, @RequestParam int index) {
        DocumentSession session = documentSessionService.getDocumentSession(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().updateCursorByIndex(userId, index);
            return ResponseEntity.ok(Map.of("message", "Cursor updated"));
        }
    }

    @PostMapping({"/{documentId}/comment"})
    public ResponseEntity<?> addComment(@PathVariable String documentId, @RequestParam String userId, @RequestParam String content, @RequestParam int startIndex, @RequestParam int endIndex) {
        DocumentSession session = documentSessionService.getDocumentSession(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().addCommentFromIndexRange(userId, content, startIndex, endIndex);
            return ResponseEntity.ok(Map.of("message", "Comment added"));
        }
    }

    @PostMapping({"/{documentId}/comment/delete"})
    public ResponseEntity<?> deleteComment(@PathVariable String documentId, @RequestParam String commentId) {
        DocumentSession session = documentSessionService.getDocumentSession(documentId);
        if (session == null) {
            throw new IllegalArgumentException("Invalid documentId");
        } else {
            session.getDocCRDT().deleteComment(commentId);
            return ResponseEntity.ok(Map.of("message", "Comment deleted"));
        }
    }

    @GetMapping({"/{documentId}/comments"})
    public ResponseEntity<?> getAllComments(@PathVariable String documentId) {
        DocumentSession session = documentSessionService.getDocumentSession(documentId);
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