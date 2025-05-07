// Source code is decompiled from a .class file using FernFlower decompiler.
package com.editor.backend.service;

import com.editor.backend.model.CRDTNode;
import com.editor.backend.model.Comment;
import com.editor.backend.model.Cursor;
import com.editor.backend.model.Operation;
import com.editor.backend.model.Operation.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CRDTService {
    private final String ROOT_ID = "root";
    private final Map<String, CRDTNode> nodeMap = new HashMap();
    private final Map<String, Stack<Operation>> undoStacks = new HashMap();
    private final Map<String, Stack<Operation>> redoStacks = new HashMap();
    private final Map<String, Cursor> userCursors = new HashMap();
    private final Map<String, Comment> commentMap = new HashMap();

    public CRDTService() {
        this.nodeMap.put("root", new CRDTNode("root", (String)null, '#', false, 0L, "system", System.currentTimeMillis()));
    }

    public void insert(char value, String parentId, String userId, long clock) {
        String id = userId + ":" + clock;
        CRDTNode node = new CRDTNode(id, parentId, value, false, clock, userId, System.currentTimeMillis());
        this.nodeMap.put(id, node);
        CRDTNode parent = (CRDTNode)this.nodeMap.get(parentId);
        if (parent != null) {
            parent.getChildren().add(id);
            this.sortChildren(parent);
        }

        Operation op = new Operation(Type.INSERT, id, parentId, value, clock, userId);
        ((Stack)this.undoStacks.computeIfAbsent(userId, (k) -> {
            return new Stack();
        })).push(op);
        this.redoStacks.computeIfAbsent(userId, (k) -> {
            return new Stack();
        });
    }

    public void delete(String id) {
        CRDTNode node = (CRDTNode)this.nodeMap.get(id);
        if (node != null && !node.isDeleted()) {
            node.setDeleted(true);
            Operation op = new Operation(Type.DELETE, id, node.getParentId(), node.getValue(), node.getLamportClock(), node.getUserId());
            ((Stack)this.undoStacks.computeIfAbsent(node.getUserId(), (k) -> {
                return new Stack();
            })).push(op);
            this.redoStacks.computeIfAbsent(node.getUserId(), (k) -> {
                return new Stack();
            });
            this.removeCommentsRelatedToNode(id);
        }

    }

    public void undo(String userId) {
        Stack<Operation> stack = (Stack)this.undoStacks.get(userId);
        if (stack != null && !stack.isEmpty()) {
            Operation op = (Operation)stack.pop();
            ((Stack)this.redoStacks.get(userId)).push(op);
            if (op.getType() == Type.INSERT) {
                ((CRDTNode)this.nodeMap.get(op.getNodeId())).setDeleted(true);
            } else {
                ((CRDTNode)this.nodeMap.get(op.getNodeId())).setDeleted(false);
            }

        }
    }

    public void redo(String userId) {
        Stack<Operation> stack = (Stack)this.redoStacks.get(userId);
        if (stack != null && !stack.isEmpty()) {
            Operation op = (Operation)stack.pop();
            ((Stack)this.undoStacks.get(userId)).push(op);
            if (op.getType() == Type.INSERT) {
                ((CRDTNode)this.nodeMap.get(op.getNodeId())).setDeleted(false);
            } else {
                ((CRDTNode)this.nodeMap.get(op.getNodeId())).setDeleted(true);
            }

        }
    }

    public void insertAtCursor(char value, String userId, long clock) {
        String parentId;
        if (!this.userCursors.containsKey(userId)) {
            parentId = "root";
        } else {
            Cursor cursor = (Cursor)this.userCursors.get(userId);
            parentId = cursor.getNodeId();
        }

        this.insert(value, parentId, userId, clock);
        String newId = userId + ":" + clock;
        this.updateCursor(userId, newId);
    }

    public void updateCursor(String userId, String nodeId) {
        if (this.nodeMap.containsKey(nodeId)) {
            List<String> ordered = new ArrayList();
            this.dfsCollectIds("root", ordered, false);
            int visualIndex = ordered.indexOf(nodeId);
            Cursor cursor = new Cursor(userId, nodeId, visualIndex, System.currentTimeMillis());
            this.userCursors.put(userId, cursor);
        }
    }

    public void updateCursorByIndex(String userId, int index) {
        List<String> ordered = new ArrayList();
        this.dfsCollectIds("root", ordered, false);
        if (index >= 0 && index < ordered.size()) {
            String nodeId = (String)ordered.get(index);
            this.updateCursor(userId, nodeId);
        } else {
            this.updateCursor(userId, "root");
        }
    }

    public int getCursorIndex(String userId) {
        Cursor c = (Cursor)this.userCursors.get(userId);
        return c != null ? c.getVisualIndex() : -1;
    }

    public Cursor getCursor(String userId) {
        return (Cursor)this.userCursors.get(userId);
    }

    public Map<String, Cursor> getAllCursors() {
        return new HashMap(this.userCursors);
    }

    public String getDocument() {
        StringBuilder sb = new StringBuilder();
        this.dfs("root", sb, new HashSet());
        System.out.println("[DEBUG] Document content: " + sb.toString());
        return sb.toString();
    }

    private void dfs(String nodeId, StringBuilder sb, Set<String> visited) {
        if (!visited.contains(nodeId)) {
            visited.add(nodeId);
            CRDTNode node = (CRDTNode)this.nodeMap.get(nodeId);
            if (!nodeId.equals("root") && !node.isDeleted()) {
                sb.append(node.getValue());
            }

            Iterator var5 = node.getChildren().iterator();

            while(var5.hasNext()) {
                String childId = (String)var5.next();
                this.dfs(childId, sb, visited);
            }

        }
    }

    private void dfsCollectIds(String nodeId, List<String> ids, boolean includeDeleted) {
        this.dfsCollectIds(nodeId, ids, includeDeleted, new HashSet());
    }

    private void dfsCollectIds(String nodeId, List<String> ids, boolean includeDeleted, Set<String> visited) {
        if (!visited.contains(nodeId)) {
            visited.add(nodeId);
            CRDTNode node = (CRDTNode)this.nodeMap.get(nodeId);
            if (!nodeId.equals("root") && (includeDeleted || !node.isDeleted())) {
                ids.add(nodeId);
            }

            Iterator var6 = node.getChildren().iterator();

            while(var6.hasNext()) {
                String childId = (String)var6.next();
                this.dfsCollectIds(childId, ids, includeDeleted, visited);
            }

        }
    }

    private void sortChildren(CRDTNode parent) {
        parent.getChildren().sort((a, b) -> {
            CRDTNode n1 = (CRDTNode)this.nodeMap.get(a);
            CRDTNode n2 = (CRDTNode)this.nodeMap.get(b);
            int cmp = Long.compare(n1.getLamportClock(), n2.getLamportClock());
            return cmp != 0 ? cmp : n1.getUserId().compareTo(n2.getUserId());
        });
    }

    public String getNodeIdByIndex(int index) {
        List<String> ordered = new ArrayList();
        this.dfsCollectIds("root", ordered, false);
        return index >= 0 && index < ordered.size() ? (String)ordered.get(index) : null;
    }

    public String getTextBetweenIndices(int startIndex, int endIndex) {
        List<String> ordered = new ArrayList();
        this.dfsCollectIds("root", ordered, false);
        if (startIndex >= 0 && endIndex < ordered.size() && startIndex <= endIndex) {
            StringBuilder sb = new StringBuilder();

            for(int i = startIndex; i <= endIndex; ++i) {
                CRDTNode node = (CRDTNode)this.nodeMap.get(ordered.get(i));
                if (node != null && !node.isDeleted()) {
                    sb.append(node.getValue());
                }
            }

            return sb.toString();
        } else {
            return "";
        }
    }

    public void paste(String text, String userId, long startingClock) {
        String parentId = this.userCursors.containsKey(userId) ? ((Cursor)this.userCursors.get(userId)).getNodeId() : "root";
        char[] var6 = text.toCharArray();
        int var7 = var6.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            char c = var6[var8];
            this.insert(c, parentId, userId, startingClock++);
            parentId = userId + ":" + (startingClock - 1L);
        }

        String newCursorId = userId + ":" + (startingClock - 1L);
        this.updateCursor(userId, newCursorId);
    }

    public String copy(String startNodeId, String endNodeId) {
        List<String> ordered = new ArrayList();
        this.dfsCollectIds("root", ordered, false);
        int start = ordered.indexOf(startNodeId);
        int end = ordered.indexOf(endNodeId);
        if (start != -1 && end != -1 && start <= end) {
            StringBuilder sb = new StringBuilder();

            for(int i = start; i <= end; ++i) {
                CRDTNode node = (CRDTNode)this.nodeMap.get(ordered.get(i));
                if (node != null && !node.isDeleted()) {
                    sb.append(node.getValue());
                }
            }

            return sb.toString();
        } else {
            return "";
        }
    }

    public void addComment(Comment comment) {
        this.commentMap.put(comment.getId(), comment);
    }

    public void addCommentFromIndexRange(String userId, String content, int startIndex, int endIndex) {
        String startNodeId = this.getNodeIdByIndex(startIndex);
        String endNodeId = this.getNodeIdByIndex(endIndex);
        String selectedText = this.getTextBetweenIndices(startIndex, endIndex);
        if (startNodeId != null && endNodeId != null) {
            Comment comment = new Comment(UUID.randomUUID().toString(), userId, content, startNodeId, endNodeId, System.currentTimeMillis(), startIndex, endIndex, selectedText);
            this.addComment(comment);
        }
    }

    public List<Comment> getAllComments() {
        return new ArrayList(this.commentMap.values());
    }

    public void deleteComment(String id) {
        this.commentMap.remove(id);
    }

    public void resolveComment(String id) {
        Comment c = (Comment)this.commentMap.get(id);
        if (c != null) {
            c.setResolved(true);
        }

    }

    private void removeCommentsRelatedToNode(String deletedNodeId) {
        List<String> ordered = new ArrayList();
        this.dfsCollectIds("root", ordered, true);
        int deletedIndex = ordered.indexOf(deletedNodeId);
        if (deletedIndex != -1) {
            this.commentMap.values().removeIf((comment) -> {
                int start = ordered.indexOf(comment.getStartNodeId());
                int end = ordered.indexOf(comment.getEndNodeId());
                return start != -1 && end != -1 && deletedIndex >= start && deletedIndex <= end;
            });
        }
    }

    public void simulateDocumentInsert(String content, String userId) {
        long clock = System.currentTimeMillis();
        char[] var5 = content.toCharArray();
        int var6 = var5.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            char c = var5[var7];
            this.insertAtCursor(c, userId, clock++);
        }

        System.out.println("Simulated insert complete. Parsed content: " + this.getDocument());
    }
}