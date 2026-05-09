package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.ast.Jump;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;

class NodeCloneTreeTest {

    @Test
    void clonesPlainNodeTreeWithFreshIdentities() {
        Node parent = new Node(Token.BLOCK);
        Node child1 = new Node(Token.EMPTY);
        Node child2 = new Node(Token.EMPTY);
        parent.addChildToBack(child1);
        parent.addChildToBack(child2);
        parent.setLineColumnNumber(7, 3);
        parent.putIntProp(Node.LABEL_ID_PROP, 42);

        Node copy = parent.cloneTree();

        assertNotSame(parent, copy);
        assertEquals(parent.getType(), copy.getType());
        assertEquals(7, copy.getLineno());
        assertEquals(3, copy.getColumn());
        assertEquals(42, copy.getIntProp(Node.LABEL_ID_PROP, -1));
        assertNull(copy.getNext());

        Node copyChild1 = copy.getFirstChild();
        Node copyChild2 = copyChild1.getNext();
        assertNotSame(child1, copyChild1);
        assertNotSame(child2, copyChild2);
        assertSame(copyChild2, copy.getLastChild());
        assertNull(copyChild2.getNext());
    }

    @Test
    void rewritesIntraSubtreeJumpTargets() {
        // BLOCK { TARGET t; GOTO -> t; }
        Node block = new Node(Token.BLOCK);
        Node target = Node.newTarget();
        target.labelId(99);
        Jump goto_ = new Jump(Token.GOTO);
        goto_.target = target;
        block.addChildToBack(target);
        block.addChildToBack(goto_);

        Node copy = block.cloneTree();

        Node copyTarget = copy.getFirstChild();
        Jump copyGoto = (Jump) copyTarget.getNext();

        assertNotSame(target, copyTarget);
        assertNotSame(goto_, copyGoto);
        // Jump target rewritten to the copy.
        assertSame(copyTarget, copyGoto.target);
        // TARGET label id reset for fresh allocation.
        assertEquals(-1, copyTarget.labelId());
        // Original is untouched.
        assertSame(target, goto_.target);
        assertEquals(99, target.labelId());
    }

    @Test
    void preservesExternalJumpTargets() {
        // External target lives outside the cloned subtree.
        Node externalTarget = Node.newTarget();
        Node block = new Node(Token.BLOCK);
        Jump breakJump = new Jump(Token.GOTO);
        breakJump.target = externalTarget;
        block.addChildToBack(breakJump);

        Node copy = block.cloneTree();
        Jump copyBreak = (Jump) copy.getFirstChild();

        // External target is preserved (shared with the original).
        assertSame(externalTarget, copyBreak.target);
    }

    @Test
    void rewritesAllJumpFields() {
        // TRY with both target (catch) and target2 (finally) inside the subtree.
        Node tryBlock = new Node(Token.BLOCK);
        Jump tryNode = new Jump(Token.TRY);
        Node catchTarget = Node.newTarget();
        Node finallyTarget = Node.newTarget();
        tryNode.target = catchTarget;
        tryNode.setFinally(finallyTarget);
        tryBlock.addChildToBack(tryNode);
        tryBlock.addChildToBack(catchTarget);
        tryBlock.addChildToBack(finallyTarget);

        Node copy = tryBlock.cloneTree();
        Jump copyTry = (Jump) copy.getFirstChild();
        Node copyCatch = copyTry.getNext();
        Node copyFinally = copyCatch.getNext();

        assertSame(copyCatch, copyTry.target);
        assertSame(copyFinally, copyTry.getFinally());
    }

    @Test
    void rewritesBreakJumpStatementLink() {
        // LABEL { ... BREAK -> LABEL }
        Node block = new Node(Token.BLOCK);
        Jump label = new Jump(Token.LABEL);
        Node labelTarget = Node.newTarget();
        label.target = labelTarget;
        Jump breakJump = new Jump(Token.BREAK);
        breakJump.setJumpStatement(label);
        block.addChildToBack(label);
        block.addChildToBack(breakJump);
        block.addChildToBack(labelTarget);

        Node copy = block.cloneTree();
        Jump copyLabel = (Jump) copy.getFirstChild();
        Jump copyBreak = (Jump) copyLabel.getNext();
        Node copyLabelTarget = copyBreak.getNext();

        assertSame(copyLabel, copyBreak.getJumpStatement());
        assertSame(copyLabelTarget, copyLabel.target);
    }

    @Test
    void rewritesLocalBlockProp() {
        // The Node-valued LOCAL_BLOCK_PROP must be remapped when both nodes are in the subtree.
        Node container = new Node(Token.BLOCK);
        Node localBlock = new Node(Token.LOCAL_BLOCK);
        Node user = new Node(Token.LOCAL_BLOCK);
        user.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
        container.addChildToBack(localBlock);
        container.addChildToBack(user);

        Node copy = container.cloneTree();
        Node copyLocal = copy.getFirstChild();
        Node copyUser = copyLocal.getNext();

        assertSame(copyLocal, copyUser.getProp(Node.LOCAL_BLOCK_PROP));
    }

    @Test
    void clonesSpecializedSubclasses() {
        Node block = new Node(Token.BLOCK);
        NumberLiteral n = new NumberLiteral(3.5);
        Name name = new Name(0, "x");
        block.addChildToBack(n);
        block.addChildToBack(name);

        Node copy = block.cloneTree();
        NumberLiteral copyNum = (NumberLiteral) copy.getFirstChild();
        Name copyName = (Name) copyNum.getNext();

        assertNotSame(n, copyNum);
        assertEquals(3.5, copyNum.getNumber());
        assertNotSame(name, copyName);
        assertEquals("x", copyName.getIdentifier());
    }

    @Test
    void resetsYieldLabelIds() {
        Node block = new Node(Token.BLOCK);
        Node yield = new Node(Token.YIELD);
        yield.labelId(7);
        block.addChildToBack(yield);

        Node copy = block.cloneTree();
        Node copyYield = copy.getFirstChild();

        assertEquals(-1, copyYield.labelId());
        assertEquals(7, yield.labelId(), "original is untouched");
    }

    @Test
    void rejectsUnsupportedSubclass() {
        // Anonymous Node subclass without shallowCopy override should throw on cloneTree().
        Node bad = new Node(Token.EMPTY) {
                    /* no override */
                };
        assertThrows(UnsupportedOperationException.class, bad::cloneTree);
    }

    @Test
    void clonedTreeIsIndependentOfOriginal() {
        Node block = new Node(Token.BLOCK);
        Node child = new Node(Token.EMPTY);
        block.addChildToBack(child);

        Node copy = block.cloneTree();
        // Mutating the copy must not affect the original.
        copy.removeChild(copy.getFirstChild());
        assertTrue(block.hasChildren());
        assertSame(child, block.getFirstChild());
    }
}
