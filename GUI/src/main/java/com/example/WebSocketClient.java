package com.example;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.example.model.Cursor;
import com.example.model.Operation;



import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class WebSocketClient {
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String documentId;
    private String userId;
    private TextArea textArea;
    private DocPage docPage;
//    private Map<String, ActiveUser> activeUsers = new HashMap<>();
    private boolean initializing = true;
    private Consumer<Map<String, Cursor>> cursorUpdateHandler;
    private Consumer<Map<String, String>> userConnectHandler;

    private static final String WEBSOCKET_URL = "ws://localhost:8080/ws";

    public WebSocketClient(String documentId, TextArea textArea, DocPage docPage) {
        this.documentId = documentId;
        this.textArea = textArea;
        this.docPage = docPage;

        // Initialize STOMP client
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    public void setUserConnectHandler(Consumer<Map<String, String>> handler) {
        this.userConnectHandler = handler;
    }

    public void setCursorUpdateHandler(Consumer<Map<String, Cursor>> handler) {
        this.cursorUpdateHandler = handler;
    }

    public void connect() {
        try {
            StompSessionHandler sessionHandler = new EditorStompSessionHandler();
            stompSession = stompClient.connect(WEBSOCKET_URL, sessionHandler).get();
            System.out.println("Connected to WebSocket server");
        } catch (InterruptedException | ExecutionException e) {
            showError("WebSocket Connection Error", "Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            System.out.println("Disconnected from WebSocket server");
        }
    }

    public void connectUser() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/connectUser/" + documentId, null);
        }
    }

    public void getDocument() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/getDocument/" + documentId, null);
        }
    }

    public void getCursors() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/getCursors/" + documentId, null);
        }
    }

//    public void updateCursor(String nodeId) {
//        if (userId != null && stompSession != null && stompSession.isConnected()) {
//            Operation operation = new Operation();
//            operation.setUserId(userId);
//            operation.setNodeId(nodeId);
//            operation.setType(Operation.OperationType.CURSOR);
//
//            stompSession.send("/app/cursorUpdate/" + documentId, operation);
//        }
//    }

    public void sendTextOperation(Operation.Type type, char value, String nodeId, long clock) {
        if (userId != null && stompSession != null && stompSession.isConnected()) {
            Operation operation = new Operation(type, nodeId, "", value, clock, userId);
            stompSession.send("/app/updateDocument/" + documentId, operation);
        }
    }

    public void insert(char value, long clock) {
        sendTextOperation(Operation.Type.INSERT, value, null, clock);
    }

    public void delete(String nodeId) {
        sendTextOperation(Operation.Type.DELETE, null, nodeId, 0);
    }

    public void undo() {
        sendTextOperation(Operation.Type.UNDO, null, null, 0);
    }

    public void redo() {
        sendTextOperation(Operation.Type.REDO, null, null, 0);
    }

    public void paste(char value, long clock) {
        sendTextOperation(Operation.Type.PASTE, value, null, clock);
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private class EditorStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("Connected to WebSocket server: " + session.getSessionId());

            // Subscribe to document updates
            session.subscribe("/topic/Document/" + documentId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Object.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(payload));

                        if (jsonNode.has("userId") && jsonNode.has("name")) {
                            // User connected event
                            String newUserId = jsonNode.get("userId").asText();
                            String userName = jsonNode.get("name").asText();

                            Map<String, String> userInfo = new HashMap<>();
                            userInfo.put("userId", newUserId);
                            userInfo.put("name", userName);

                            if (userId == null) {
                                userId = newUserId;
                                System.out.println("Set userId: " + userId);

                                // Once we have our userId, get the document and cursors
                                getDocument();
                                getCursors();
                            }

                            // Update active users
                            if (userConnectHandler != null) {
                                Platform.runLater(() -> userConnectHandler.accept(userInfo));
                            }
                        } else if (jsonNode.has("type")) {
                            // Operation event
                            Operation operation = objectMapper.readValue(objectMapper.writeValueAsString(payload), Operation.class);
                            handleOperation(operation);
                        }
                    } catch (Exception e) {
                        System.err.println("Error handling document update: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // Subscribe to full document content
            session.subscribe("/topic/Document/" + documentId + "/full", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    String documentContent = (String) payload;

                    Platform.runLater(() -> {
                        initializing = true;
                        docPage.setTextAreaContent(documentContent);
                        initializing = false;
                    });
                }
            });

            // Subscribe to cursor updates
            session.subscribe("/topic/Document/" + documentId + "/cursors", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Object.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(payload));
                        Operation operation = objectMapper.readValue(objectMapper.writeValueAsString(payload), Operation.class);

                        // Only update if it's not our own cursor
                        if (!operation.getUserId().equals(userId)) {
                            // Handle cursor update
                            System.out.println("Remote cursor update: " + operation.getUserId() + " at " + operation.getNodeId());
                            // Update cursor positions in the UI
                            // This will be handled by the DocPage
                        }
                    } catch (Exception e) {
                        System.err.println("Error handling cursor update: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // Subscribe to initial cursor positions
            session.subscribe("/topic/Document/" + documentId + "/cursors/init", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Object.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    try {
                        Map<String, Cursor> cursors = objectMapper.readValue(
                                objectMapper.writeValueAsString(payload),
                                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Cursor.class)
                        );

                        if (cursorUpdateHandler != null) {
                            Platform.runLater(() -> cursorUpdateHandler.accept(cursors));
                        }
                    } catch (Exception e) {
                        System.err.println("Error handling initial cursors: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // Connect user to document after subscriptions are set up
            connectUser();
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            showError("WebSocket Error", "Error: " + exception.getMessage());
            exception.printStackTrace();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            showError("WebSocket Transport Error", "Transport error: " + exception.getMessage());
            exception.printStackTrace();
        }

        private void handleOperation(Operation operation) {
            // Don't process our own operations that come back from the server
            if (userId != null && operation.getUserId().equals(userId)) {
                return;
            }

            Platform.runLater(() -> {
                System.out.println("Received operation: " + operation.getType() + " from " + operation.getUserId());

                switch (operation.getType()) {
                    case INSERT:
                    case PASTE:
                    case DELETE:
                    case UNDO:
                    case REDO:
                        // These operations are handled by the server's CRDT
                        // The full document will be updated separately
                        break;

                    case COMMENT:
                        // Handle comment operations
                        // DocPage will need to update its comment display
                        break;
                }
            });
        }
    }
}