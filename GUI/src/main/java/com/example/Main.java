package com.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main extends Application {
    private Stage primaryStage;
    private DocPage docPage;  // Reference to the DocPage

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showHomePage();
    }

    public void showHomePage() {
        HomePage homePage = new HomePage(this);
        Scene scene = new Scene(homePage.getRoot(), 500, 400);
        primaryStage.setTitle("Collaborative Text Editor - Home");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showDocPage(boolean isEditor, String documentId, String editorCode, String viewerCode) {
        docPage = new DocPage(this, isEditor, documentId, editorCode, viewerCode); // Save reference to DocPage
        Scene scene = new Scene(docPage.getRoot(), 800, 600);
        primaryStage.setTitle("Collaborative Text Editor - Document");
        primaryStage.setScene(scene);
    }

    // Getter for DocPage reference
    public DocPage getDocPage() {
        return docPage;  // Return the reference to DocPage
    }

    // Getter for the primaryStage
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // Method to read the content of a file
    public String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}