// Source code is decompiled from a .class file using FernFlower decompiler.
package com.editor.backend.service;

import com.editor.backend.model.DocumentSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DocumentSessionService {
    private final Map<String, DocumentSession> documentSessions = new HashMap();

    public DocumentSessionService() {
    }

    public DocumentSession getDocumentSession(String documentId) {
        return (DocumentSession)this.documentSessions.get(documentId);
    }

    public void addDocumentSession(String documentId, DocumentSession documentSession) {
        this.documentSessions.put(documentId, documentSession);
    }

    public void removeDocumentSession(String documentId) {
        this.documentSessions.remove(documentId);
    }

    public boolean documentSessionExists(String documentId) {
        return this.documentSessions.containsKey(documentId);
    }

    public DocumentSession findDocumentSessionByCode(String inputCode) {
        for (Map.Entry<String, DocumentSession> entry : documentSessions.entrySet()) {
            if (entry.getValue().getEditorCode().equals(inputCode)) {
                return entry.getValue();
            }
            else if (entry.getValue().getViewerCode().equals(inputCode)) {
                return entry.getValue();
            }
        }
        return null; // Return null if no match is found
    }

}