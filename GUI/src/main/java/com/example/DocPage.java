package com.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Popup;

public class DocPage {
    private BorderPane root;
    private StackPane rootWrapper;
    private Main mainApp;
    private boolean isEditor;
    private String editorCode;
    private String viewerCode;
    private String documentId;
    private String userId;
    private TextArea textArea;
    private TextArea lineNumbers;

    private Button addCommentBtn = new Button("Add Comment");
    private Popup commentPopup = new Popup();
    private final List<Comment> comments = new ArrayList<>();
    private VBox commentDisplayBox = new VBox(10); // New comment display panel
    private VBox activeUsersBox = new VBox(10); // Active users section

    public DocPage(Main mainApp, boolean isEditor, String documentId, String userId, String editorCode, String viewerCode) {
        this.mainApp = mainApp;
        this.isEditor = isEditor;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.documentId = documentId;
        this.userId = userId;

        root = new BorderPane();
        rootWrapper = new StackPane(root);

        HBox navbar = createNavbar();
        root.setTop(navbar);

        HBox contentArea = new HBox();
        contentArea.setSpacing(10);
        contentArea.setPadding(new Insets(10));

        VBox editorArea = createEditorArea();
        setupCommentButton();

        // Setting up comment display panel
        commentDisplayBox.setPadding(new Insets(10));
        commentDisplayBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: lightgray;");
        commentDisplayBox.setPrefWidth(250);
        Label commentHeader = new Label("Comments");
        commentHeader.setFont(Font.font("Arial", 16));
        commentDisplayBox.getChildren().add(commentHeader);

        // Setting up active users panel
        activeUsersBox.setPadding(new Insets(10));
        activeUsersBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: lightgray;");
        activeUsersBox.setPrefWidth(250);
        Label usersHeader = new Label("Active Users");
        usersHeader.setFont(Font.font("Arial", 16));
        activeUsersBox.getChildren().add(usersHeader);

        // Add both sections to contentArea
        contentArea.getChildren().addAll(editorArea, commentDisplayBox, activeUsersBox);
        HBox.setHgrow(editorArea, Priority.ALWAYS);

        root.setCenter(contentArea);
    }

    private HBox createNavbar() {
        HBox navbar = new HBox(20);
        navbar.setPadding(new Insets(10));
        navbar.setAlignment(Pos.CENTER);

        Button undoBtn = new Button("Undo");
        Button redoBtn = new Button("Redo");
        Button exportBtn = new Button("Export as Text");

        HBox codesBox = new HBox(10);
        codesBox.setAlignment(Pos.CENTER);
        if (isEditor) {
            codesBox.getChildren().addAll(createCodeBox("Editor Code", editorCode), createCodeBox("Viewer Code", viewerCode));
        } else {
            codesBox.getChildren().add(createCodeBox("Viewer Code", viewerCode));
        }

        HBox searchBar = new HBox(5);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search text...");
        Button findBtn = new Button("Find");

        findBtn.setOnAction(e -> findText(searchField.getText()));
        exportBtn.setOnAction(e -> exportText());

        searchBar.getChildren().addAll(new Label("Search:"), searchField, findBtn);
        navbar.getChildren().addAll(undoBtn, redoBtn, exportBtn, codesBox, searchBar);

        return navbar;
    }

    private VBox createEditorArea() {
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(10));
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setFillWidth(true);

        lineNumbers = new TextArea("1");
        lineNumbers.setEditable(false);
        lineNumbers.setFocusTraversable(false);
        lineNumbers.setPrefWidth(35);
        lineNumbers.setStyle("-fx-control-inner-background: #f4f4f4; -fx-font-size: 14; -fx-border-color: transparent; -fx-focus-color: transparent;");
        lineNumbers.setFont(Font.font("Courier New", 14));

        textArea = new TextArea();
        textArea.setFont(Font.font("Arial", 14));
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(80);
        textArea.setStyle("-fx-font-size: 14;");
        if (!isEditor) textArea.setEditable(false);

        textArea.scrollTopProperty().addListener((obs, oldVal, newVal) -> lineNumbers.setScrollTop((double) newVal));
        textArea.textProperty().addListener((obs, oldText, newText) -> updateLineNumbers(newText));
        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> highlightCurrentLine());

        HBox editorBox = new HBox(lineNumbers, textArea);
        editorBox.setSpacing(0);
        editorBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textArea, Priority.ALWAYS);

        // Synchronize heights of textArea and comment/active users sections
        editorBox.prefHeightProperty().bind(centerBox.heightProperty().multiply(0.9));
        lineNumbers.prefHeightProperty().bind(editorBox.heightProperty());
        textArea.prefHeightProperty().bind(editorBox.heightProperty());

        centerBox.getChildren().add(editorBox);
        return centerBox;
    }

    private void updateLineNumbers(String newText) {
        String[] lines = newText.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines.length; i++) {
            sb.append(i).append("\n");
        }
        lineNumbers.setText(sb.toString());
        highlightCurrentLine();
    }

    private void highlightCurrentLine() {
        int caretPos = textArea.getCaretPosition();
        String[] lines = textArea.getText().split("\n", -1);

        int currentLine = 1;
        int charCount = 0;
        for (int i = 0; i < lines.length; i++) {
            charCount += lines[i].length() + 1;
            if (caretPos < charCount) {
                currentLine = i + 1;
                break;
            }
        }

        StringBuilder highlightedNumbers = new StringBuilder();
        for (int i = 1; i <= lines.length; i++) {
            if (i == currentLine) {
                highlightedNumbers.append("▶ ").append(i).append(" ◀\n");
            } else {
                highlightedNumbers.append("  ").append(i).append("\n");
            }
        }
        lineNumbers.setText(highlightedNumbers.toString());
    }

    private void findText(String query) {
        if (query.isEmpty()) return;

        String text = textArea.getText();
        int index = text.indexOf(query);

        if (index >= 0) {
            textArea.requestFocus();
            textArea.selectRange(index, index + query.length());
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Text not found.");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private void exportText() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(mainApp.getPrimaryStage());

        if (file != null) {
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "File saved successfully!");
                alert.setHeaderText(null);
                alert.showAndWait();
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving file: " + ex.getMessage());
                alert.setHeaderText(null);
                alert.showAndWait();
            }
        }
    }

    private VBox createCodeBox(String label, String code) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        HBox codeRow = new HBox(2);
        TextField codeField = new TextField(code);
        codeField.setEditable(false);
        codeField.setPrefWidth(80);
        Button copyBtn = new Button("Copy");

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(code);
            clipboard.setContent(content);
        });

        codeRow.getChildren().addAll(codeField, copyBtn);
        box.getChildren().addAll(lbl, codeRow);
        return box;
    }

    public void setTextAreaContent(String text) {
        textArea.setText(text);
    }

    public StackPane getRoot() {
        return rootWrapper;
    }

    private void setupCommentButton() {
        addCommentBtn.setStyle("-fx-background-color: #ffd700; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.2), 4, 0, 0, 2);");
        addCommentBtn.setVisible(false);

        StackPane overlay = new StackPane(addCommentBtn);
        overlay.setPickOnBounds(false);
        overlay.setAlignment(Pos.TOP_LEFT);
        rootWrapper.getChildren().add(overlay);

        textArea.selectedTextProperty().addListener((obs, oldSel, newSel) -> {
            if (!newSel.isEmpty()) {
                addCommentBtn.setVisible(true);
                Platform.runLater(() -> {
                    int caretPos = textArea.getCaretPosition();
                    int lineIndex = textArea.getText().substring(0, caretPos).split("\n", -1).length;
                    double yOffset = lineIndex * 18;
                    addCommentBtn.setTranslateY(yOffset - textArea.getScrollTop() + 40);
                    addCommentBtn.setTranslateX(200);
                });
            } else {
                addCommentBtn.setVisible(false);
            }
        });

        addCommentBtn.setOnAction(e -> {
            String selectedText = textArea.getSelectedText().trim();
            if (selectedText.isEmpty()) return;

            int startIndex = textArea.getSelection().getStart();
            int endIndex = textArea.getSelection().getEnd();

            TextArea commentInput = new TextArea();
            commentInput.setPromptText("Enter your comment...");
            commentInput.setWrapText(true);
            commentInput.setPrefRowCount(3);
            commentInput.setPrefColumnCount(25);

            Button saveBtn = new Button("Save");
            Button cancelBtn = new Button("Cancel");
            HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            VBox commentBox = new VBox(10, new Label("Add a Comment:"), commentInput, buttonBox);
            commentBox.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-padding: 10;");
            commentBox.setPrefWidth(300);

            commentPopup.getContent().clear();
            commentPopup.getContent().add(commentBox);

            double x = addCommentBtn.localToScreen(addCommentBtn.getBoundsInLocal()).getMinX();
            double y = addCommentBtn.localToScreen(addCommentBtn.getBoundsInLocal()).getMaxY();
            commentPopup.show(addCommentBtn, x, y + 5);

            cancelBtn.setOnAction(ev -> commentPopup.hide());

            saveBtn.setOnAction(ev -> {
                String commentText = commentInput.getText().trim();
                if (!commentText.isEmpty()) {
                    sendCommentToServer(startIndex, endIndex, selectedText, commentText);
                }
                commentPopup.hide();
            });
        });
    }

    private void updateCommentDisplay() {
        commentDisplayBox.getChildren().removeIf(node -> !(node instanceof Label));
    
        List<Comment> currentComments = new ArrayList<>(comments);
    
        for (Comment comment : currentComments) {
            int startLine = getLineNumberFromPosition(comment.getStartIndex());
            int endLine = getLineNumberFromPosition(comment.getEndIndex());
    
            String lineInfo;
            if (startLine == endLine) {
                lineInfo = "Line: " + startLine;
            } else {
                lineInfo = "Lines: " + startLine + " - " + endLine;
            }
    
            TextArea ta = new TextArea();
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setText("[" + comment.getSelectedText() + "]\n" + comment.getContent() + "\n" + lineInfo);
            ta.setStyle("-fx-font-size: 12; -fx-control-inner-background: #f0f0f0;");
            ta.setPrefHeight(80);
            ta.setMaxWidth(200);
    
            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: #ff4c4c; -fx-text-fill: white; -fx-font-weight: bold;");
    
            deleteBtn.setOnAction(e -> {
                comments.remove(comment);
                updateCommentDisplay();
            });
    
            VBox commentBox = new VBox(5, ta, deleteBtn);
            commentDisplayBox.getChildren().add(commentBox);
        }
    }

    private int getLineNumberFromPosition(int position) {
        String[] lines = textArea.getText().split("\n", -1);
        int charCount = 0;
        int lineNumber = 1;
        for (String line : lines) {
            charCount += line.length() + 1;
            if (position < charCount) {
                break;
            }
            lineNumber++;
        }
        return lineNumber;
    }
    private void sendCommentToServer(int startIndex, int endIndex, String selectedText, String commentText) {
        String apiUrl = "http://localhost:8080/{documentId}/comment"; // Replace {documentId} dynamically
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            String.format("userId=%s&content=%s&startIndex=%d&endIndex=%d",
                                    "dynamicUserId", // Replace with userId from API
                                    URLEncoder.encode(commentText, StandardCharsets.UTF_8),
                                    startIndex, endIndex)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Comment added successfully!");
                                alert.setHeaderText(null);
                                alert.showAndWait();
                                fetchCommentsFromServer(); // Refresh comments
                            });
                        } else {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add comment: " + response.body());
                                alert.setHeaderText(null);
                                alert.showAndWait();
                            });
                        }
                    });
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error sending comment: " + ex.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
    private void fetchCommentsFromServer() {
        String apiUrl = "http://localhost:8080/{documentId}/comments"; // Replace {documentId} dynamically
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Platform.runLater(() -> {
                                List<Comment> fetchedComments = parseComments(response.body());
                                comments.clear();
                                comments.addAll(fetchedComments);
                                updateCommentDisplay();
                            });
                        } else {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to fetch comments: " + response.body());
                                alert.setHeaderText(null);
                                alert.showAndWait();
                            });
                        }
                    });
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error fetching comments: " + ex.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
    private List<Comment> parseComments(String response) {
        List<Comment> commentList = new ArrayList<>();
        try {
            // Split the response into lines (each line represents a comment)
            String[] lines = response.split("\n");

            // Iterate through each line (skip the header if present)
            for (String line : lines) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Split the line into fields using the delimiter (e.g., "|")
                String[] fields = line.split("\\|");

                // Ensure the line has the correct number of fields
                if (fields.length == 4) {
                    int startIndex = Integer.parseInt(fields[0].trim());
                    int endIndex = Integer.parseInt(fields[1].trim());
                    String selectedText = fields[2].trim();
                    String content = fields[3].trim();

                    // Create a new Comment object and add it to the list
                    Comment comment = new Comment(startIndex, endIndex, selectedText, content);
                    commentList.add(comment);
                }
            }
        } catch (Exception e) {
            // Handle parsing exceptions
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error parsing comments: " + e.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();
        }
        return commentList;
    }

}