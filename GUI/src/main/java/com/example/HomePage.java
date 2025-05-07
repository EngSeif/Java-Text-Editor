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
                    mainApp.showDocPage(true, jsonResponse.getString("documentId"), jsonResponse.getString("userId"), jsonResponse.getString("editorCode"), jsonResponse.getString("viewerCode"));
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
                                jsonResponse.getString("userId"),
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
            try {
                // Get entered code
                String enteredCode = codeField.getText();

                // Prepare the JSON request body
                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("enteredCode", enteredCode);

                // Prepare the HTTP client and request
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/documents/userJoin/"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                        .build();

                // Send request and get response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("User entered code: " + enteredCode);
                System.out.println("Server response: " + response.body());

                int statusCode = response.statusCode();

                // Handle 404 status code for invalid code
                if (statusCode == 404) {
                    System.out.println("Code Not Valid");
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid code. Please try again.");
                    alert.setHeaderText("Join Error");
                    alert.showAndWait();
                } else if (statusCode >= 200 && statusCode < 300) {
                    // Parse JSON response for successful status codes (2xx)
                    JSONObject jsonResponse = new JSONObject(response.body());
                    String role = jsonResponse.getString("role");
                    String documentId = jsonResponse.getString("documentId");
                    String userId = jsonResponse.getString("userId");

                    // Check if the role is valid
                    if (role.equalsIgnoreCase("none")) {
                        System.out.println("Code Not Valid");
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid code. Please try again.");
                        alert.setHeaderText("Join Error");
                        alert.showAndWait();
                    } else {
                        // Show document page for valid role
                        System.out.println("Role: " + role + ", Document ID: " + documentId);
                        mainApp.showDocPage(role.equalsIgnoreCase("editor"), documentId, userId,"Hidden", "Hidden");
                    }
                } else {
                    // Handle other non-2xx status codes
                    System.out.println("Unexpected error: " + statusCode);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "An unexpected error occurred. Status code: " + statusCode);
                    alert.setHeaderText("Join Error");
                    alert.showAndWait();
                }

            } catch (Exception ex) {
                // Handle any exceptions and print error
                System.out.println("Error: " + ex.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR, "An error occurred: " + ex.getMessage());
                alert.setHeaderText("Error");
                alert.showAndWait();
            }
        });
        joinBox.getChildren().addAll(codeField, joinBtn);
        joinBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, createBtn, importBtn, new Label("Or join with a share code:"), joinBox);
    }

    public VBox getRoot() {
        return root;
    }



}