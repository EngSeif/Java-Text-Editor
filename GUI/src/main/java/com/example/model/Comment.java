// Source code is decompiled from a .class file using FernFlower decompiler.
package com.example.model;

public class Comment {
    private String id;
    private String userId;
    private String content;
    private String startNodeId;
    private String endNodeId;
    private long timestamp;
    private boolean resolved;
    private int startIndex;
    private int endIndex;
    private String selectedText;

    public Comment(String id, String userId, String content, String startNodeId, String endNodeId, long timestamp, int startIndex, int endIndex, String selectedText) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
        this.timestamp = timestamp;
        this.resolved = false;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.selectedText = selectedText;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStartNodeId() {
        return this.startNodeId;
    }

    public void setStartNodeId(String startNodeId) {
        this.startNodeId = startNodeId;
    }

    public String getEndNodeId() {
        return this.endNodeId;
    }

    public void setEndNodeId(String endNodeId) {
        this.endNodeId = endNodeId;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isResolved() {
        return this.resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public String getSelectedText() {
        return this.selectedText;
    }
}