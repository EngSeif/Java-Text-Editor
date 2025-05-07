package com.editor.backend.model;
import java.util.UUID;
import com.editor.backend.service.CRDTService;
import java.util.List;


public class DocumentSession {
    private String documentId;
    private String editorCode;
    private String viewerCode;
    private CRDTService docCRDT;
    private List<User> docUsers;

    public DocumentSession () {
        this.documentId = UUID.randomUUID().toString();
        this.editorCode = "E-" + UUID.randomUUID().toString().substring(0, 8);
        this.viewerCode = "V-" + UUID.randomUUID().toString().substring(0, 8);
        this.docCRDT = new CRDTService();
    }

    public boolean isEditor(String code) {
        return code.equals(this.editorCode);
    }

    public boolean isViewer(String code) {
        return code.equals(this.viewerCode);
    }

    public String getViewerCode() {
        return this.viewerCode;
    }

    public String getEditorCode() {
        return this.editorCode;
    }

    public String getDocId() {
        return this.documentId;
    }

    //* Maybe Change Later
    public CRDTService getDocCRDT() {
        return this.docCRDT;
    }

    public void addToUsers(User newUser) {
        this.docUsers.add(newUser);
    }
}
