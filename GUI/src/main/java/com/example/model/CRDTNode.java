package com.example.model;
import java.util.ArrayList;
import java.util.List;

/**
 * A CRDTNode represents a single character in the document.
 * Each node knows where it was inserted (parentId),
 * and maintains a list of children (for tree structure).
 */
public class CRDTNode {

    // Unique identifier of this character (e.g., "u1:4")
    private String id;

    // ID of the character this was inserted after
    private String parentId;

    // The actual character (e.g., 'a', 'b', etc.)
    private char value;

    // True if the character has been logically deleted (tombstone)
    private boolean deleted;

    // Lamport timestamp when inserted (used for causal ordering)
    private long lamportClock;

    // The user who inserted this character
    private String userId;

    // Real-world time when the node was created (optional UI purposes)
    private long createdAt;

    // List of IDs of children inserted after this node (preserves ordering)
    private List<String> children;

    // --- Constructor ---
    public CRDTNode(String id, String parentId, char value, boolean deleted, long lamportClock, String userId, long createdAt) {
        this.id = id;
        this.parentId = parentId;
        this.value = value;
        this.deleted = deleted;
        this.lamportClock = lamportClock;
        this.userId = userId;
        this.createdAt = createdAt;
        this.children = new ArrayList<>();
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public char getValue() {
        return value;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public long getLamportClock() {
        return lamportClock;
    }

    public String getUserId() {
        return userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public List<String> getChildren() {
        return children;
    }

    // --- Setters ---
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
