package org.mozilla.javascript.tests.commentspr465;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;

/** @author ravik @@since 08/08/18 2:31 PM */
public class LoopCommentsTest {

    @Test
    public void inlinecommentForSt() {
        String inputStr =
                "function f1() {\n"
                        + "/*\n"
                        + "lksjlsdjf lsjdkflsjdf */\n"
                        + "for(i=0; i<10; i++) //test For comment\n"
                        + "{\n"
                        + "    var j = i + 10; //test in formy =\n"
                        + "}\n"
                        + "}";

        AstNode scriptRoot =
                CommentsTestUtils.getRhinoASTRootNode(inputStr, "forloop1", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        Node firstNode = funcDef.getBody().getFirstChild();
        Assert.assertEquals(Token.COMMENT, firstNode.getType());

        ForLoop forNode = (ForLoop) firstNode.getNext();
        Assert.assertEquals(
                "//test For comment", ((Comment) forNode.getInlineComment()).getValue());
        Assert.assertEquals(
                "//test in formy =",
                ((Comment) ((AstNode) (forNode.getBody().getFirstChild())).getInlineComment())
                        .getValue());
    }

    @Test
    public void inlinecommentForStWithSingleSt() {
        String inputStr =
                "function f1() {\n"
                        + "for(i=0; i<10; i++) //test For comment\n"
                        + "    var j = i + 10; //test in formy =\n"
                        + "}";

        AstNode scriptRoot =
                CommentsTestUtils.getRhinoASTRootNode(inputStr, "forloop2", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        ForLoop forNode = (ForLoop) funcDef.getBody().getFirstChild();
        Assert.assertEquals(
                "//test For comment", ((Comment) forNode.getInlineComment()).getValue());

        Assert.assertEquals(
                "//test in formy =", ((Comment) forNode.getBody().getInlineComment()).getValue());
    }

    @Test
    public void inlinecommentWhileWithSingleSt() {
        String inputStr =
                "function f1() {\n"
                        + "while(i<10) //test For comment\n"
                        + "    var j = i + 10; //test in while =\n"
                        + "}";

        AstNode scriptRoot =
                CommentsTestUtils.getRhinoASTRootNode(inputStr, "whileloop1", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        WhileLoop whileNode = (WhileLoop) funcDef.getBody().getFirstChild();
        Assert.assertEquals(
                "//test For comment", ((Comment) whileNode.getInlineComment()).getValue());

        Assert.assertEquals(
                "//test in while =", ((Comment) whileNode.getBody().getInlineComment()).getValue());
    }

    @Test
    public void inlinecommentDoWhileWithSingleSt() {
        String inputStr =
                "function f1() {\n"
                        + "do //test do comment\n"
                        + "{\n"
                        + "    var j = i + 10; //test in do-while =\n"
                        + "} while(i<10); //test do-while condition comment\n"
                        + "}";

        AstNode scriptRoot =
                CommentsTestUtils.getRhinoASTRootNode(inputStr, "doWhileLoop1", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        DoLoop doWhileNode = (DoLoop) funcDef.getBody().getFirstChild();
        Assert.assertEquals(
                "//test do comment", ((Comment) doWhileNode.getInlineComment()).getValue());

        Assert.assertEquals(
                "//test in do-while =",
                ((Comment) ((AstNode) (doWhileNode.getBody().getFirstChild())).getInlineComment())
                        .getValue());

        Assert.assertEquals(
                "//test do-while condition comment", ((Comment) doWhileNode.getNext()).getValue());
    }

    @Test
    public void inlinecommentDoWhileWithSingleStWithouSemi() {
        String inputStr =
                "function f1() {\n"
                        + "do //test do comment\n"
                        + "{\n"
                        + "    var j = i + 10; //test in do-while =\n"
                        + "} while(i<10) //test do-while condition comment\n"
                        + "}";

        AstNode scriptRoot =
                CommentsTestUtils.getRhinoASTRootNode(inputStr, "doWhile2", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        DoLoop doWhileNode = (DoLoop) funcDef.getBody().getFirstChild();
        Assert.assertEquals(
                "//test do comment", ((Comment) doWhileNode.getInlineComment()).getValue());

        Assert.assertEquals(
                "//test in do-while =",
                ((Comment) ((AstNode) (doWhileNode.getBody().getFirstChild())).getInlineComment())
                        .getValue());

        Assert.assertNull(doWhileNode.getNext());
    }

    @Test
    public void inlinecommentInWith() {
        String inputStr =
                "function f1() {\n" + "with(x) //comment1\n" + "    var j = 29; //comment2\n" + "}";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "with1", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        WithStatement withNode = (WithStatement) funcDef.getBody().getFirstChild();
        Assert.assertEquals("//comment1", ((Comment) withNode.getInlineComment()).getValue());

        Assert.assertEquals(
                "//comment2", ((Comment) withNode.getStatement().getInlineComment()).getValue());
    }
}
