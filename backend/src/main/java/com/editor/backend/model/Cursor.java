package com.editor.backend.model;

public class Cursor {
    private String userId;       // who owns this cursor
    private String nodeId;       // which CRDT node it points to
    private int visualIndex;     // optional (can be -1 if not computed)
    private long updatedAt;      // timestamp of last move (ms)

    public Cursor(String userId, String nodeId, int visualIndex, long updatedAt) {
        this.userId = userId;
        this.nodeId = nodeId;
        this.visualIndex = visualIndex;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public String getNodeId() { return nodeId; }
    public int getVisualIndex() { return visualIndex; }
    public long getUpdatedAt() { return updatedAt; }

    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public void setVisualIndex(int visualIndex) { this.visualIndex = visualIndex; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
