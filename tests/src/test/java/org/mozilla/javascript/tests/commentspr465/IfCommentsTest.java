package org.mozilla.javascript.tests.commentspr465;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;

/** @author ravik @@since 08/08/18 12:01 PM */
public class IfCommentsTest {

    @Test
    public void inlineCommentsForIf() {
        String inputStr =
                "function f1() {\n"
                        + "// alert(type);\n"
                        + "if ($scope.disableEdit) // if comment\n"
                        + "    $scope.type = ''; //Dont show\n"
                        + "else //else\n"
                        + "    $scope.type = type;\n"
                        + "//abcdef\n"
                        + "}";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "ifelse1", null, null);

        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();
        Node comm1Node = funcDef.getBody().getFirstChild();
        Assert.assertEquals(Token.COMMENT, comm1Node.getType());

        IfStatement ifSt = (IfStatement) comm1Node.getNext();
        AstNode ifInlineComm = ifSt.getInlineComment();
        Assert.assertNotNull(ifInlineComm);
        Assert.assertEquals("// if comment", ((Comment) ifInlineComm).getValue());

        AstNode thenPartNode = ifSt.getThenPart();
        Assert.assertNotNull(thenPartNode.getInlineComment());
        Assert.assertEquals("//Dont show", ((Comment) thenPartNode.getInlineComment()).getValue());

        Assert.assertNotNull(ifSt.getElseKeyWordInlineComment());
        Assert.assertEquals("//else", ((Comment) ifSt.getElseKeyWordInlineComment()).getValue());

        Node comm2Node = ifSt.getNext();
        Assert.assertEquals(Token.COMMENT, comm2Node.getType());
    }

    @Test
    public void blockCommentinElse() {
        String inputStr =
                "function f1() {\n"
                        + "if(x == 8)\n"
                        + "    y = 9;\n"
                        + "else //test else\n"
                        + "{ //else part\n"
                        + "    u = 9 + 10;\n"
                        + "} //7800\n"
                        + "//9034\n"
                        + "}";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "ifelse2", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        IfStatement ifSt = (IfStatement) funcDef.getBody().getFirstChild();
        Assert.assertNotNull(ifSt.getElseKeyWordInlineComment());
        Assert.assertEquals(
                "//test else", ((Comment) ifSt.getElseKeyWordInlineComment()).getValue());

        AstNode elseNode = ifSt.getElsePart();
        Assert.assertEquals(Token.BLOCK, elseNode.getType());
        Node comm1Node = elseNode.getFirstChild();
        Assert.assertEquals(Token.COMMENT, comm1Node.getType());
        Assert.assertEquals("//else part", ((Comment) comm1Node).getValue());

        Node comm2Node = ifSt.getNext();
        Assert.assertEquals(Token.COMMENT, comm2Node.getType());
        Assert.assertEquals("//7800", ((Comment) comm2Node).getValue());

        Node comm3Node = comm2Node.getNext();
        Assert.assertEquals(Token.COMMENT, comm3Node.getType());
        Assert.assertEquals("//9034", ((Comment) comm3Node).getValue());
    }

    @Test
    public void commentInbetweenIfElse() {
        String inputStr =
                "function f1() {\n"
                        + "if(a == 3) {\n"
                        + "    var x = 4;\n"
                        + "} \n"
                        + "//end of if\n"
                        + "else {\n"
                        + "    var x = 4\n"
                        + "}\n"
                        + "}";

        String outputStr =
                "if (a == 3) {\n" + "  var x = 4;\n" + "} else {\n" + "  var x = 4;\n" + "}";
        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "ifelse3", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();
        IfStatement ifSt = (IfStatement) funcDef.getBody().getFirstChild();
        Assert.assertEquals(outputStr.trim(), ifSt.toSource().trim());
    }

    @Test
    public void commentsInElseIf() {
        String inputStr =
                "function f1() {\n"
                        + "if(u == 8) //if 2\n"
                        + "    x = 6;   //then 2\n"
                        + "else if (y == 8)//else if 2\n"
                        + "    o = 0;\n"
                        + "else  //else 2\n"
                        + "    u = 9;  //elsepart 2\n"
                        + "// xysdflj ljk\n"
                        + "//test\n"
                        + "k = 0 + 8;\n"
                        + "}";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "ifelse4", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();

        IfStatement ifSt = (IfStatement) funcDef.getBody().getFirstChild();
        AstNode ifInlineComm = ifSt.getInlineComment();
        Assert.assertNotNull(ifInlineComm);
        Assert.assertEquals("//if 2", ((Comment) ifInlineComm).getValue());

        IfStatement elsePartIfSt = (IfStatement) ifSt.getElsePart();
        AstNode elseIfInlineComm = elsePartIfSt.getInlineComment();
        Assert.assertNotNull(elseIfInlineComm);
        Assert.assertEquals("//else if 2", ((Comment) elseIfInlineComm).getValue());

        Assert.assertNotNull(elsePartIfSt.getElseKeyWordInlineComment());
        Assert.assertEquals(
                "//else 2", ((Comment) elsePartIfSt.getElseKeyWordInlineComment()).getValue());

        AstNode elseNode = elsePartIfSt.getElsePart();
        Assert.assertEquals("//elsepart 2", ((Comment) elseNode.getInlineComment()).getValue());

        Node comm2Node = ifSt.getNext();
        Assert.assertEquals(Token.COMMENT, comm2Node.getType());
        Assert.assertEquals("// xysdflj ljk", ((Comment) comm2Node).getValue());

        Node comm3Node = comm2Node.getNext();
        Assert.assertEquals(Token.COMMENT, comm3Node.getType());
        Assert.assertEquals("//test", ((Comment) comm3Node).getValue());
    }
}
