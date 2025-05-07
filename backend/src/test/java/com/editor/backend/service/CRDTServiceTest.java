package com.editor.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.editor.backend.model.Comment;
import com.editor.backend.model.Cursor;
public class CRDTServiceTest {

    private CRDTService crdt;

    @BeforeEach
    void setup() {
        crdt = new CRDTService();
    }

    @Test
    void testSingleInsert() {
        crdt.insert('A', "root", "u1", 1);
        assertEquals("A", crdt.getDocument());
    }

    @Test
    void testInsertMultipleDescendingOrder() {
        crdt.insert('A', "root", "u1", 5);
        crdt.insert('B', "root", "u1", 4);
        crdt.insert('C', "root", "u1", 3);
        assertEquals("ABC", crdt.getDocument());
    }

    @Test
    void testInsertOutOfOrderClock() {
        crdt.insert('C', "root", "u1", 3);
        crdt.insert('A', "root", "u1", 1);
        crdt.insert('B', "root", "u1", 2);
        assertEquals("CBA", crdt.getDocument()); // sorted descending
    }

    @Test
    void testInsertFromDifferentUsers() {
        crdt.insert('A', "root", "u1", 1);
        crdt.insert('B', "root", "u2", 2);
        crdt.insert('C', "root", "u3", 3);
        assertEquals("CBA", crdt.getDocument()); // descending by clock
    }

    @Test
    void testDeleteThenUndoThenRedo() {
        crdt.insert('X', "root", "u1", 1);
        crdt.insert('Y', "u1:1", "u1", 2);
        crdt.delete("u1:2");
        assertEquals("X", crdt.getDocument());

        crdt.undo("u1");
        assertEquals("XY", crdt.getDocument());

        crdt.redo("u1");
        assertEquals("X", crdt.getDocument());
    }

    @Test
    void testUndoMultipleLevels() {
        crdt.insert('P', "root", "u1", 1);
        crdt.insert('Q', "u1:1", "u1", 2);
        crdt.insert('R', "u1:2", "u1", 3);
        crdt.undo("u1");
        crdt.undo("u1");
        assertEquals("P", crdt.getDocument());

        crdt.redo("u1");
        crdt.redo("u1");
        assertEquals("PQR", crdt.getDocument());
    }

    @Test
    void testInsertAtCursorSingle() {
        crdt.insert('A', "root", "u1", 1);
        crdt.updateCursor("u1", "u1:1");
        crdt.insertAtCursor('B', "u1", 2);
        assertEquals("AB", crdt.getDocument());
    }

    @Test
    void testInsertAtCursorMultipleUsers() {
        crdt.insert('X', "root", "u1", 1);
        crdt.updateCursor("u1", "u1:1");
        crdt.insertAtCursor('A', "u1", 2); // u1:2 after X

        crdt.updateCursor("u2", "u1:1");
        crdt.insertAtCursor('B', "u2", 1); // u2:1 after X

        assertEquals("XAB", crdt.getDocument()); // clock descending → A (2), B (1)
    }

    @Test
    void testRepeatedCursorUpdate() {
        crdt.insert('A', "root", "u1", 1);
        crdt.updateCursor("u1", "u1:1");
        crdt.updateCursor("u1", "u1:1");
        crdt.insertAtCursor('B', "u1", 2);
        crdt.updateCursor("u1", "u1:2");
        crdt.insertAtCursor('C', "u1", 3);
        assertEquals("ABC", crdt.getDocument());
    }

    @Test
    void testTombstoneNotRendered() {
        crdt.insert('Z', "root", "u1", 1);
        crdt.delete("u1:1");
        assertEquals("", crdt.getDocument());
    }

    @Test
    void testCursorFallbackToRoot() {
        crdt.insertAtCursor('M', "u1", 1); // no prior cursor → inserts after root
        assertEquals("M", crdt.getDocument());
    }

    @Test
    void testCursorIndexComputation() {
        crdt.insert('A', "root", "u1", 1);
        crdt.insert('B', "u1:1", "u1", 2);
        crdt.insert('C', "u1:2", "u1", 3);
        crdt.updateCursor("u1", "u1:2");
        assertEquals(1, crdt.getCursorIndex("u1")); // index of B
    }

    @Test
    void testInsertConflictWithSameClockDifferentUsers() {
        crdt.insert('A', "root", "u1", 1);
        crdt.insert('B', "root", "u2", 1); // same clock as A
        crdt.insert('C', "root", "u3", 2);

        assertEquals("CAB", crdt.getDocument()); // C(2), then A/B resolved by userId: A < B
    }
    @Test
void testPasteMultipleCharacters() {
    crdt.insert('A', "root", "u1", 1);
    crdt.insert('B', "u1:1", "u1", 2);
    crdt.updateCursor("u1", "u1:2");

    crdt.paste("XYZ", "u1", 3);

    assertEquals("ABXYZ", crdt.getDocument());
    assertEquals("u1:5", crdt.getCursor("u1").getNodeId());
}

@Test
void testCopyRangeValid() {
    crdt.insert('A', "root", "u1", 1);
    crdt.insert('B', "u1:1", "u1", 2);
    crdt.insert('C', "u1:2", "u1", 3);

    String copied = crdt.copy("u1:1", "u1:3");
    assertEquals("ABC", copied);
}

@Test
void testCopyRangePartial() {
    crdt.insert('A', "root", "u1", 1);
    crdt.insert('B', "u1:1", "u1", 2);
    crdt.insert('C', "u1:2", "u1", 3);
    crdt.delete("u1:2");

    String copied = crdt.copy("u1:1", "u1:3");
    assertEquals("AC", copied); // B is tombstoned, excluded
}

@Test
void testPasteAndUndo() {
    crdt.insert('A', "root", "u1", 1);
    crdt.updateCursor("u1", "u1:1");
    crdt.paste("MN", "u1", 2); // M = u1:2, N = u1:3

    assertEquals("AMN", crdt.getDocument());

    crdt.undo("u1"); // removes N
    crdt.undo("u1"); // removes M

    assertEquals("A", crdt.getDocument());
}
//@Test
//void testAddAndRetrieveComment() {
//    crdt.insert('A', "root", "u1", 1);
//    crdt.insert('B', "u1:1", "u1", 2);
//    Comment comment = new Comment("c1", "u1", "Important note", "u1:1", "u1:2", System.currentTimeMillis());
//    crdt.addComment(comment);
//    assertEquals(1, crdt.getAllComments().size());
//    assertEquals("Important note", crdt.getAllComments().get(0).getContent());
//}
//
//@Test
//void testResolveComment() {
//    Comment c = new Comment("c2", "u1", "To fix", "u1:1", "u1:2", System.currentTimeMillis());
//    crdt.addComment(c);
//    crdt.resolveComment("c2");
//    assertTrue(crdt.getAllComments().get(0).isResolved());
//}
//
//@Test
//void testDeleteComment() {
//    Comment c = new Comment("c3", "u1", "Outdated", "u1:1", "u1:2", System.currentTimeMillis());
//    crdt.addComment(c);
//    crdt.deleteComment("c3");
//    assertEquals(0, crdt.getAllComments().size());
//}
//
//@Test
//void testAutoRemoveCommentOnNodeDelete() {
//    crdt.insert('X', "root", "u1", 1);
//    crdt.insert('Y', "u1:1", "u1", 2);
//    Comment c = new Comment("c4", "u1", "Watch this", "u1:1", "u1:2", System.currentTimeMillis());
//    crdt.addComment(c);
//    crdt.delete("u1:2");
//    assertEquals(0, crdt.getAllComments().size());
//}
@Test
void testUpdateCursorByIndexAndInsert() {
    crdt.insert('A', "root", "u1", 1); // index 0
    crdt.insert('B', "u1:1", "u1", 2); // index 1
    crdt.insert('C', "u1:2", "u1", 3); // index 2

    crdt.updateCursorByIndex("u1", 1);
    Cursor cursor = crdt.getCursor("u1");

    assertEquals("u1:2", cursor.getNodeId()); // 'B' is u1:2

    crdt.insertAtCursor('X', "u1", 4);
    assertEquals("ABXC", crdt.getDocument());
}


}
