package org.mozilla.javascript.tests.commentspr465;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;

/** @author ravik @@since 07/08/18 12:36 PM */
public class SwitchCommentsTest {

    @Test
    public void inlineCommentForSwitch() {
        String inputStr =
                "function f1() {\n"
                        + "    switch(a) //switch inline\n"
                        + "    {\n"
                        + "        //Switch comment\n"
                        + "        case 1:\n"
                        + "    }\n"
                        + "}";

        String expectedStr =
                "function f1() {\n" + "  switch (a) {\n" + "    case 1:\n" + "  }\n" + "}\n";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "switch1", null, null);
        Assert.assertEquals(expectedStr, scriptRoot.toSource());
    }

    @Test
    public void inlineCommentForCase() {
        String inputStr =
                "switch(a) {\n"
                        + "    case 1:\n"
                        + "    case 2: //testcase\n"
                        + "    case 3:\n"
                        + "}";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "switch2", null, null);
        List<SwitchCase> caseNodes = assertSwitch(scriptRoot, 3);
        Assert.assertNotNull(caseNodes.get(1).getInlineComment());
        Assert.assertEquals(
                "//testcase", ((Comment) caseNodes.get(1).getInlineComment()).getValue());
    }

    @Test
    public void commentsInCaseBlock() {
        String inputStr =
                "switch(a) {\n"
                        + "    case 6:\n"
                        + "        var x = 3;\n"
                        + "        /*\n"
                        + "            case6 block comment\n"
                        + "        */\n"
                        + "        var y = x + 1 + a;\n"
                        + "        break;\n"
                        + "        //End of case 6\n"
                        + "    case 7:\n"
                        + "}\n";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "switch3", null, null);
        List<SwitchCase> caseNodes = assertSwitch(scriptRoot, 2);
        SwitchCase case6 = caseNodes.get(0);
        Assert.assertEquals(5, case6.getStatements().size());
        AstNode comment1St = case6.getStatements().get(1);
        Assert.assertEquals(Token.COMMENT, comment1St.getType());
        Assert.assertEquals(
                "/*\n" + "            case6 block comment\n" + "        */",
                ((Comment) comment1St).getValue());

        AstNode comment2St = case6.getStatements().get(4);
        Assert.assertEquals(Token.COMMENT, comment2St.getType());
        Assert.assertEquals("//End of case 6", ((Comment) comment2St).getValue());
    }

    @Test
    public void inlineCommentForCaseBlock() {
        String inputStr =
                "switch(a) {\n"
                        + "    case 8:\n"
                        + "    case 9: { //inline comment at block start\n"
                        + "        var x = 3;\n"
                        + "        var y = x + 1 + a;\n"
                        + "        break;\n"
                        + "        //End of case 9\n"
                        + "    }\n"
                        + "    case 10:\n"
                        + "}";
        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "switch4", null, null);
        List<SwitchCase> caseNodes = assertSwitch(scriptRoot, 3);
        SwitchCase case9 = caseNodes.get(1);
        Assert.assertTrue(
                case9.getStatements().size() == 1
                        && case9.getStatements().get(0).getType() == Token.BLOCK);
        Node firstCommentChild = ((Scope) case9.getStatements().get(0)).getFirstChild();
        Assert.assertEquals(Token.COMMENT, firstCommentChild.getType());
        Assert.assertEquals(
                Token.COMMENT, firstCommentChild.getNext().getNext().getNext().getNext().getType());
    }

    @Test
    public void commentsInEmptyCase() {
        String inputStr =
                "switch(a) {\n"
                        + "case 10:\n"
                        + "case 11:\n"
                        + "    var x = 3; //inline comment\n"
                        + "    var y = x + 1 + a;\n"
                        + "    break;\n"
                        + "case 12:\n"
                        + "    //case 12 inline comment\n"
                        + "case 13:\n"
                        + "    var x = 3 //case 3\n"
                        + "    var y = x + 1 + a;\n"
                        + "    break;\n"
                        + "}";
        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "switch5", null, null);
        List<SwitchCase> caseNodes = assertSwitch(scriptRoot, 4);

        SwitchCase case11 = caseNodes.get(1);
        AstNode comm1Node = case11.getStatements().get(0).getInlineComment();
        Assert.assertNotNull(comm1Node);
        Assert.assertEquals("//inline comment", ((Comment) comm1Node).getValue());

        SwitchCase case12 = caseNodes.get(2);
        AstNode comm2Node = case12.getStatements().get(0);
        Assert.assertNotNull(comm1Node);
        Assert.assertEquals("//case 12 inline comment", ((Comment) comm2Node).getValue());

        SwitchCase case13 = caseNodes.get(3);
        AstNode comm3Node = case13.getStatements().get(0);
        Assert.assertNotNull(comm3Node);
        Assert.assertNull(comm3Node.getInlineComment());
        Assert.assertEquals("var x = 3;\n", comm3Node.toSource());
    }

    @Test
    public void mixedComments() {
        String inputStr =
                "switch(a) {\n"
                        + "case 14:\n"
                        + "case 15:\n"
                        + "    var x = 3;\n"
                        + "    //case 14 line comment\n"
                        + "    var y = x + 1 + a;\n"
                        + "    break;\n"
                        + "case 16:\n"
                        + "    /*\n"
                        + "        case16 blcok comment\n"
                        + "    */\n"
                        + "case 17: //in comment\n"
                        + "{\n"
                        + "    var x = 3;\n"
                        + "    var y = x + 1 + a;\n"
                        + "    break;\n"
                        + "    //End of case 17\n"
                        + "}\n"
                        + "}";

        AstNode scriptRoot = CommentsTestUtils.getRhinoASTRootNode(inputStr, "switch6", null, null);
        List<SwitchCase> caseNodes = assertSwitch(scriptRoot, 4);

        SwitchCase case15 = caseNodes.get(1);
        AstNode comm1Node = case15.getStatements().get(1);
        Assert.assertNotNull(comm1Node);
        Assert.assertEquals("//case 14 line comment", ((Comment) comm1Node).getValue());

        SwitchCase case16 = caseNodes.get(2);
        AstNode comm2Node = case16.getStatements().get(0);
        Assert.assertNotNull(comm1Node);
        Assert.assertEquals(Token.COMMENT, comm2Node.getType());

        SwitchCase case17 = caseNodes.get(3);
        Assert.assertNotNull(case17.getInlineComment());
        Assert.assertEquals("//in comment", ((Comment) case17.getInlineComment()).getValue());
        Assert.assertEquals(Token.BLOCK, case17.getStatements().get(0).getType());
    }

    private static List<SwitchCase> assertSwitch(AstNode scriptRoot, int noOfCaseNodes) {
        Assert.assertNotNull(scriptRoot);
        Node firstChild = scriptRoot.getFirstChild();
        Assert.assertNotNull(firstChild);
        Assert.assertEquals(Token.SWITCH, firstChild.getType());
        SwitchStatement switchNode = (SwitchStatement) firstChild;
        List<SwitchCase> caseNodes = switchNode.getCases();
        Assert.assertTrue(caseNodes.size() == noOfCaseNodes);
        return caseNodes;
    }
}
