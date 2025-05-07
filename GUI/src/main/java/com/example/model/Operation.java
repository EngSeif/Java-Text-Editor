package com.example.model;

public class Operation {
    public enum Type {
        INSERT, DELETE, UNDO, REDO, PASTE, COMMENT
    }

    private Type type;
    private String nodeId;
    private String parentId;
    private char value;
    private long clock;
    private String userId;

    public Operation(Type type, String nodeId, String parentId, char value, long clock, String userId) {
        this.type = type;
        this.nodeId = nodeId;
        this.parentId = parentId;
        this.value = value;
        this.clock = clock;
        this.userId = userId;
    }

    public Type getType() {
        return this.type;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getParentId() {
        return this.parentId;
    }

    public char getValue() {
        return this.value;
    }

    public long getClock() {
        return this.clock;
    }

    public String getUserId() {
        return this.userId;
    }
}
