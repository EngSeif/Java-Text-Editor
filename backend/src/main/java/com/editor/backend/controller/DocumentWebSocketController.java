// Source code is decompiled from a .class file using FernFlower decompiler.
package com.editor.backend.controller;

import com.editor.backend.controller.DocumentWebSocketController;
import com.editor.backend.model.Cursor;
import com.editor.backend.model.DocumentSession;
import com.editor.backend.model.Operation;
import com.editor.backend.model.User;
import com.editor.backend.service.CRDTService;
import com.editor.backend.service.DocumentSessionService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class DocumentWebSocketController {
    private final DocumentSessionService documentSessionService;

    public DocumentWebSocketController(DocumentSessionService documentSessionService) {
        this.documentSessionService = documentSessionService;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        System.out.println("[WebSocket] User connected.");
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        System.out.println("[WebSocket] User disconnected.");
    }

    @MessageMapping({"/connectUser/{documentId}"})
    @SendTo({"/topic/Document/{documentId}"})
    public Map<String, String> connectUser(@DestinationVariable String documentId) {
        User user = new User("editor");
        this.documentSessionService.getDocumentSession(documentId).addToUsers(user);
        Map<String, String> response = new HashMap();
        response.put("userId", user.getUserId());
        response.put("name", "Anonymous");
        return response;
    }

    @MessageMapping({"/updateDocument/{documentId}"})
    @SendTo({"/topic/Document/{documentId}"})
    public Operation updateDocument(@DestinationVariable String documentId, Operation operation) {
        DocumentSession session = this.documentSessionService.getDocumentSession(documentId);
        CRDTService crdt = session.getDocCRDT();
        switch (operation.getType().ordinal()) {
            case 1:
                crdt.insertAtCursor(operation.getValue(), operation.getUserId(), operation.getClock());
                break;
            case 2:
                crdt.delete(operation.getNodeId());
                break;
            case 3:
                crdt.undo(operation.getUserId());
                break;
            case 4:
                crdt.redo(operation.getUserId());
                break;
            case 5:
                crdt.paste(String.valueOf(operation.getValue()), operation.getUserId(), operation.getClock());
                break;
            case 6:
                System.out.println("[WebSocket] COMMENT type received, but not handled yet.");
                break;
            default:
                System.out.println("[WebSocket] Unknown operation type: " + String.valueOf(operation.getType()));
        }

        return operation;
    }

    @MessageMapping({"/cursorUpdate/{documentId}"})
    @SendTo({"/topic/Document/{documentId}/cursors"})
    public Operation updateCursor(@DestinationVariable String documentId, Operation operation) {
        this.documentSessionService.getDocumentSession(documentId).getDocCRDT().updateCursor(operation.getUserId(), operation.getNodeId());
        return operation;
    }

    @MessageMapping({"/getDocument/{documentId}"})
    @SendTo({"/topic/Document/{documentId}/full"})
    public String getDocument(@DestinationVariable String documentId) {
        return this.documentSessionService.getDocumentSession(documentId).getDocCRDT().getDocument();
    }

    @MessageMapping({"/getCursors/{documentId}"})
    @SendTo({"/topic/Document/{documentId}/cursors/init"})
    public Map<String, Cursor> getAllCursors(@DestinationVariable String documentId) {
        return this.documentSessionService.getDocumentSession(documentId).getDocCRDT().getAllCursors();
    }
}