package com.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;


public class HomePage {
    private VBox root;
    private Main mainApp;

    public HomePage(Main mainApp) {
        this.mainApp = mainApp;
        root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Collaborative Text Editor");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button createBtn = new Button("Create New Document");
        createBtn.setOnAction(e -> {
            try {
                // Create an HttpClient
                HttpClient client = HttpClient.newHttpClient();

                // Create a request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/documents"))
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.noBody()) // Use noBody for a POST without body
                        .build();

                // Send the request and get the response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Check response status code
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    // Correctly extract the body from the response
                    String responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody); // Parse the body content

                    // Use the extracted values from the JSON response
                    mainApp.showDocPage(true, jsonResponse.getString("documentId"), jsonResponse.getString("editorCode"), jsonResponse.getString("viewerCode"));
                } else {
                    System.out.println("Error: " + statusCode);
                }

            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        });
        Button importBtn = new Button("Import Document");
        importBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(mainApp.getPrimaryStage());

            if (selectedFile != null) {
                try {
                    // Read file content as UTF-8 string
                    String fileContent = Files.readString(selectedFile.toPath(), StandardCharsets.UTF_8);  // <-- updated helper method below

                    // Prepare HTTP client
                    HttpClient client = HttpClient.newHttpClient();

                    // Prepare JSON payload
                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("fileContent", fileContent);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8081/api/documents/upload"))
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    int statusCode = response.statusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        String responseBody = response.body();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        mainApp.showDocPage(true,
                                jsonResponse.getString("documentId"),
                                jsonResponse.getString("editorCode"),
                                jsonResponse.getString("viewerCode"));

                        DocPage docPage = mainApp.getDocPage();
                        docPage.setTextAreaContent(fileContent);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Server error: " + statusCode);
                        alert.setHeaderText(null);
                        alert.showAndWait();
                    }

                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error reading file: " + ex.getMessage());
                    alert.setHeaderText(null);
                    alert.showAndWait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.out.println("Request interrupted: " + ex.getMessage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Unexpected error: " + ex.getMessage());
                }
            }
        });

        HBox joinBox = new HBox(10);
        TextField codeField = new TextField();
        codeField.setPromptText("Enter Share Code");
        Button joinBtn = new Button("Join");
        joinBtn.setOnAction(e -> {
            // For demo, assume code determines role
            try {
                String enteredCode = codeField.getText();

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/documents/userJoin/" + enteredCode))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("User entered code: " + enteredCode);
                System.out.println("Server response: " + response.body());

                JSONObject jsonResponse = new JSONObject(response.body());
                String role = jsonResponse.getString("role");
                String documentId = jsonResponse.getString("documentId");
            if (role.equalsIgnoreCase("none")) {
                System.out.println("Code Not Valid");
            }
            else
            {
                mainApp.showDocPage(role.equalsIgnoreCase("editor"), documentId, generateShareCode(), generateShareCode());
            }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        });
        joinBox.getChildren().addAll(codeField, joinBtn);
        joinBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, createBtn, importBtn, new Label("Or join with a share code:"), joinBox);
    }

    public VBox getRoot() {
        return root;
    }

    private String generateShareCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }
}