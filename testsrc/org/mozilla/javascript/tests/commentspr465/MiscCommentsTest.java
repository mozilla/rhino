package org.mozilla.javascript.tests.commentspr465;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.TryStatement;

/** @author ravik @@since 08/08/18 3:13 PM */
public class MiscCommentsTest {

    @Test
    public void commentsInTryCatch() {
        String inputStr =
                "function f1() {\n"
                        + "try //try comment\n"
                        + "{\n"
                        + "    adddlert(\"Welcome guest!\");\n"
                        + "}\n"
                        + "catch(err) //catch comment\n"
                        + "{\n"
                        + "    var k =10;\n"
                        + "}\n"
                        + "}";

        String outputStr = "catch (err) {\n" + "  var k = 10;\n" + "}\n";
        AstNode scriptRoot =
                CommentsTestUtils.getRhinoASTRootNode(inputStr, "tryCatch1", null, null);
        FunctionNode funcDef = (FunctionNode) scriptRoot.getFirstChild();
        TryStatement tryNode = (TryStatement) funcDef.getBody().getFirstChild();

        Assert.assertEquals("//try comment", ((Comment) tryNode.getInlineComment()).getValue());

        CatchClause clauseNode = tryNode.getCatchClauses().get(0);
        Assert.assertEquals(outputStr, clauseNode.toSource());
    }

    @Test
    public void commentsInObjectLiteral() {
        String inputStr =
                "function f1() {\n"
                        + "var timeRegistrationLines = {\n"
                        + "    \"orderId\": a.orderId,\n"
                        + "    \"registeredQuantity\": a.datavalue,\n"
                        + "    // \"surchargeComponent1\": (a.surchargeComponent1.datavalue.surchargeComponent || null),\n"
                        + "    // \"surchargeComponent2\": (a.surchargeComponent2.datavalue.surchargeComponent || null),\n"
                        + "    \"unit\": a.unit,\n"
                        + "    \"wageCompType\": a.wageCompType,\n"
                        + "    // \"wageCompSubType2\": (a.extraWageComponentSubType.datavalue || null),\n"
                        + "    \"week\": a[a.timeLineFrom].weekNumber,\n"
                        + "    \"comments\": (a.datavalue || null)\n"
                        + "};\n"
                        + "}";

        String outputStr =
                "function f1() {\n"
                        + "  var timeRegistrationLines = {\n"
                        + "  \"orderId\": a.orderId, \n"
                        + "  \"registeredQuantity\": a.datavalue, \n"
                        + "  \"unit\": a.unit, \n"
                        + "  \"wageCompType\": a.wageCompType, \n"
                        + "  \"week\": a[a.timeLineFrom].weekNumber, \n"
                        + "  \"comments\": (a.datavalue || null)};\n"
                        + "}\n";
        AstNode scriptRoot =
                CommentsTestUtils.getRhinoASTRootNode(inputStr, "tryCatch1", null, null);
        Assert.assertEquals(outputStr, scriptRoot.toSource());
    }
}
