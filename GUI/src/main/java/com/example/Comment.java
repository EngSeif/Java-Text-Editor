package com.example;

public class Comment {
    private final int startIndex;
    private final int endIndex;
    private final String selectedText;
    private final String content;

    public Comment(int startIndex, int endIndex, String selectedText, String content) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.selectedText = selectedText;
        this.content = content;
    }

    /* === Get Start Index of Comment === */
    public int getStartIndex() {
        return startIndex;
    }

    /* === Get End Index of Comment === */
    public int getEndIndex() {
        return endIndex;
    }

    /* === Returns Selected Text to be Commented === */
    public String getSelectedText() {
        return selectedText;
    }

    /* === Returns Comment written on Text === */
    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Comment on '" + selectedText + "': " + content;
    }
}