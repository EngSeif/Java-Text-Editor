package com.editor.backend.model;
import java.util.Random;
import java.util.UUID;

public class User {
    private String userId;
    private String displayName;
    private String role; // Editor or Viewer
    private static final String[] ADJECTIVES = {
            "Swift", "Brave", "Clever", "Silent", "Fierce", "Wise", "Bold", "Lively", "Sharp", "Chill"
    };

    private static final String[] NOUNS = {
            "Lion", "Eagle", "Wizard", "Panther", "Fox", "Wolf", "Falcon", "Dragon", "Knight", "Ninja"
    };

    public User(String Role) {
        this.userId = UUID.randomUUID().toString();
        if (Role.equalsIgnoreCase("editor") || Role.equalsIgnoreCase("viewer")) {
            this.role = Role;
        } else {
            throw new IllegalArgumentException("Role must be 'editor' or 'viewer'");
        }
        this.displayName = generateRandomName();
    }

    private String generateRandomName() {
        Random random = new Random();
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[random.nextInt(NOUNS.length)];
        int number = random.nextInt(1000); // Optional number suffix
        return adjective + noun + number;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }
}
