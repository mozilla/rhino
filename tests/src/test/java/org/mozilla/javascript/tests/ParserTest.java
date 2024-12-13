/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.GeneratorMethodDefinition;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.TemplateCharacters;
import org.mozilla.javascript.ast.TemplateLiteral;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UpdateExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WithStatement;
import org.mozilla.javascript.ast.XmlFragment;
import org.mozilla.javascript.ast.XmlLiteral;
import org.mozilla.javascript.testing.TestErrorReporter;

public class ParserTest {
    CompilerEnvirons environment;

    @Before
    public void setUp() throws Exception {
        environment = new CompilerEnvirons();
        environment.setLanguageVersion(Context.VERSION_DEFAULT);
    }

    @Test
    public void autoSemiColonBetweenNames() {
        AstRoot root = parse("\nx\ny\nz\n");
        AstNode first = ((ExpressionStatement) root.getFirstChild()).getExpression();
        assertEquals("x", first.getString());
        AstNode second = ((ExpressionStatement) root.getFirstChild().getNext()).getExpression();
        assertEquals("y", second.getString());
        AstNode third =
                ((ExpressionStatement) root.getFirstChild().getNext().getNext()).getExpression();
        assertEquals("z", third.getString());
    }

    @Test
    public void parseAutoSemiColonBeforeNewlineAndComments() throws IOException {
        AstRoot root = parseAsReader("var s = 3\n" + "/* */ /*  test comment */ var t = 1;");
        assertNotNull(root.getComments());
        assertEquals(2, root.getComments().size());

        assertEquals("var s = 3;\nvar t = 1;\n", root.toSource());
    }

    @Test
    public void commentInArray() throws IOException {
        // Test a single comment
        AstRoot root = parseAsReader("var array = [/**/];");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals(root.toSource(), "var array = [];\n");
        // Test multiple comments
        root = parseAsReader("var array = [/*Hello*/ /*World*/ 1,2];");
        assertNotNull(root.getComments());
        assertEquals(2, root.getComments().size());
        assertEquals(root.toSource(), "var array = [1, 2];\n");
        // Test no comments
        root = parseAsReader("var array = [1,2];");
        assertNull(root.getComments());
        assertEquals(root.toSource(), "var array = [1, 2];\n");
        root = parseAsReader("var array = [1,/*hello*/2,/*World*/3];");
        // Test comments in middle
        assertNotNull(root.getComments());
        assertEquals(2, root.getComments().size());
        assertEquals(root.toSource(), "var array = [1, 2, 3];\n");
    }

    @Test
    public void newlineAndComments() throws IOException {
        AstRoot root = parseAsReader("var s = 3;\n" + "/* */ /* txt */var t = 1");
        assertNotNull(root.getComments());
        assertEquals(2, root.getComments().size());

        assertEquals("var s = 3;\n/* */\n\n/* txt */\n\nvar t = 1;\n", root.toSource());
    }

    @Test
    public void newlineAndCommentsFunction() {
        AstRoot root = parse("f('2345' // Second arg\n);");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
    }

    @Test
    public void newlineAndCommentsFunction2() {
        AstRoot root = parse("f('1234',\n// Before\n'2345' // Second arg\n);");
        assertNotNull(root.getComments());
        assertEquals(2, root.getComments().size());
    }

    @Test
    public void autoSemiBeforeComment1() {
        parse("var a = 1\n/** a */ var b = 2");
    }

    @Test
    public void autoSemiBeforeComment2() {
        parse("var a = 1\n/** a */\n var b = 2");
    }

    @Test
    public void autoSemiBeforeComment3() {
        parse("var a = 1\n/** a */\n /** b */ var b = 2");
    }

    @Test
    public void linenoAssign() {
        AstRoot root = parse("\n\na = b");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof Assignment);
        assertEquals(Token.ASSIGN, n.getType());
        assertLineColumnAre(2, 1, n);
    }

    @Test
    public void linenoCall() {
        AstRoot root = parse("\nfoo(123);");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof FunctionCall);
        assertEquals(Token.CALL, n.getType());
        assertLineColumnAre(1, 1, n);
    }

    @Test
    public void linenoGetProp() {
        AstRoot root = parse("\nfoo.bar");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof PropertyGet);
        assertEquals(Token.GETPROP, n.getType());
        assertLineColumnAre(1, 1, n);

        PropertyGet getprop = (PropertyGet) n;
        AstNode m = getprop.getRight();

        assertTrue(m instanceof Name);
        assertEquals(Token.NAME, m.getType()); // used to be Token.STRING!
        assertLineColumnAre(1, 5, m);
    }

    @Test
    public void linenoGetElem() {
        AstRoot root = parse("\nfoo[123]");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof ElementGet);
        assertEquals(Token.GETELEM, n.getType());
        assertLineColumnAre(1, 1, n);
    }

    @Test
    public void linenoComment() {
        AstRoot root = parse("\n/** a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(1, 1, root.getComments().first());
    }

    @Test
    public void linenoComment2() {
        AstRoot root = parse("\n/**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(1, 1, root.getComments().first());
    }

    @Test
    public void linenoComment3() {
        AstRoot root = parse("\n  \n\n/**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(3, 1, root.getComments().first());
    }

    @Test
    public void linenoComment4() {
        AstRoot root = parse("\n  \n\n  /**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(3, 3, root.getComments().first());
    }

    @Test
    public void lineComment5() {
        AstRoot root = parse("  /**\n* a.\n* b.\n* c.*/\n");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(0, 3, root.getComments().first());
    }

    @Test
    public void lineComment6() {
        AstRoot root = parse("  \n/**\n* a.\n* b.\n* c.*/\n");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(1, 1, root.getComments().first());
    }

    @Test
    public void linenoComment7() {
        AstRoot root = parse("var x;\n/**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(1, 1, root.getComments().first());
    }

    @Test
    public void linenoComment8() {
        AstRoot root = parse("\nvar x;/**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertLineColumnAre(1, 7, root.getComments().first());
    }

    @Test
    public void linenoLiteral() {
        AstRoot root =
                parse(
                        "\nvar d =\n"
                                + "    \"foo\";\n"
                                + "var e =\n"
                                + "    1;\n"
                                + "var f = \n"
                                + "    1.2;\n"
                                + "var g = \n"
                                + "    2e5;\n"
                                + "var h = \n"
                                + "    'bar';\n");

        VariableDeclaration stmt1 = (VariableDeclaration) root.getFirstChild();
        List<VariableInitializer> vars1 = stmt1.getVariables();
        VariableInitializer firstVar = vars1.get(0);
        Name firstVarName = (Name) firstVar.getTarget();
        AstNode firstVarLiteral = firstVar.getInitializer();

        VariableDeclaration stmt2 = (VariableDeclaration) stmt1.getNext();
        List<VariableInitializer> vars2 = stmt2.getVariables();
        VariableInitializer secondVar = vars2.get(0);
        Name secondVarName = (Name) secondVar.getTarget();
        AstNode secondVarLiteral = secondVar.getInitializer();

        VariableDeclaration stmt3 = (VariableDeclaration) stmt2.getNext();
        List<VariableInitializer> vars3 = stmt3.getVariables();
        VariableInitializer thirdVar = vars3.get(0);
        Name thirdVarName = (Name) thirdVar.getTarget();
        AstNode thirdVarLiteral = thirdVar.getInitializer();

        VariableDeclaration stmt4 = (VariableDeclaration) stmt3.getNext();
        List<VariableInitializer> vars4 = stmt4.getVariables();
        VariableInitializer fourthVar = vars4.get(0);
        Name fourthVarName = (Name) fourthVar.getTarget();
        AstNode fourthVarLiteral = fourthVar.getInitializer();

        VariableDeclaration stmt5 = (VariableDeclaration) stmt4.getNext();
        List<VariableInitializer> vars5 = stmt5.getVariables();
        VariableInitializer fifthVar = vars5.get(0);
        Name fifthVarName = (Name) fifthVar.getTarget();
        AstNode fifthVarLiteral = fifthVar.getInitializer();

        assertLineColumnAre(2, 5, firstVarLiteral);
        assertLineColumnAre(4, 5, secondVarLiteral);
        assertLineColumnAre(6, 5, thirdVarLiteral);
        assertLineColumnAre(8, 5, fourthVarLiteral);
        assertLineColumnAre(10, 5, fifthVarLiteral);
    }

    @Test
    public void linenoSwitch() {
        AstRoot root =
                parse(
                        "\nswitch (a) {\n"
                                + "   case\n"
                                + "     1:\n"
                                + "     b++;\n"
                                + "   case 2:\n"
                                + "   default:\n"
                                + "     b--;\n"
                                + "  }\n");

        SwitchStatement switchStmt = (SwitchStatement) root.getFirstChild();
        AstNode switchVar = switchStmt.getExpression();
        List<SwitchCase> cases = switchStmt.getCases();
        SwitchCase firstCase = cases.get(0);
        AstNode caseArg = firstCase.getExpression();
        List<AstNode> caseBody = firstCase.getStatements();
        ExpressionStatement exprStmt = (ExpressionStatement) caseBody.get(0);
        UpdateExpression incrExpr = (UpdateExpression) exprStmt.getExpression();
        AstNode incrVar = incrExpr.getOperand();

        SwitchCase secondCase = cases.get(1);
        AstNode defaultCase = cases.get(2);
        AstNode returnStmt = (AstNode) switchStmt.getNext();

        assertLineColumnAre(1, 1, switchStmt);
        assertLineColumnAre(1, 9, switchVar);
        assertLineColumnAre(2, 4, firstCase);
        assertLineColumnAre(3, 6, caseArg);
        assertLineColumnAre(4, 6, exprStmt);
        assertLineColumnAre(4, 6, incrExpr);
        assertLineColumnAre(4, 6, incrVar);
        assertLineColumnAre(5, 4, secondCase);
        assertLineColumnAre(6, 4, defaultCase);
    }

    @Test
    public void linenoFunctionParams() {
        AstRoot root =
                parse(
                        "\nfunction\n"
                                + "    foo(\n"
                                + "    a,\n"
                                + "    b,\n"
                                + "    c) {\n"
                                + "}\n");
        FunctionNode function = (FunctionNode) root.getFirstChild();
        Name functionName = function.getFunctionName();

        AstNode body = function.getBody();
        List<AstNode> params = function.getParams();
        AstNode param1 = params.get(0);
        AstNode param2 = params.get(1);
        AstNode param3 = params.get(2);

        assertLineColumnAre(1, 1, function);
        assertLineColumnAre(2, 5, functionName);
        assertLineColumnAre(3, 5, param1);
        assertLineColumnAre(4, 5, param2);
        assertLineColumnAre(5, 5, param3);
        assertLineColumnAre(5, 8, body);
    }

    @Test
    public void linenoVarDecl() {
        AstRoot root = parse("\nvar\n" + "    a =\n" + "    3\n");

        VariableDeclaration decl = (VariableDeclaration) root.getFirstChild();
        List<VariableInitializer> vars = decl.getVariables();
        VariableInitializer init = vars.get(0);
        AstNode declName = init.getTarget();
        AstNode expr = init.getInitializer();

        assertLineColumnAre(1, 1, decl);
        assertLineColumnAre(2, 5, init);
        assertLineColumnAre(2, 5, declName);
        assertLineColumnAre(3, 5, expr);
    }

    @Test
    public void linenoReturn() {
        AstRoot root =
                parse(
                        "\nfunction\n"
                                + "    foo(\n"
                                + "    a,\n"
                                + "    b,\n"
                                + "    c) {\n"
                                + "    return\n"
                                + "    4;\n"
                                + "}\n");
        FunctionNode function = (FunctionNode) root.getFirstChild();
        Name functionName = function.getFunctionName();

        AstNode body = function.getBody();
        ReturnStatement returnStmt = (ReturnStatement) body.getFirstChild();
        ExpressionStatement exprStmt = (ExpressionStatement) returnStmt.getNext();
        AstNode returnVal = exprStmt.getExpression();

        assertLineColumnAre(6, 5, returnStmt);
        assertLineColumnAre(7, 5, exprStmt);
        assertLineColumnAre(7, 5, returnVal);
    }

    @Test
    public void linenoFor() {
        AstRoot root = parse("\nfor(\n" + ";\n" + ";\n" + ") {\n" + "}\n");

        ForLoop forLoop = (ForLoop) root.getFirstChild();
        AstNode initClause = forLoop.getInitializer();
        AstNode condClause = forLoop.getCondition();
        AstNode incrClause = forLoop.getIncrement();

        assertLineColumnAre(1, 1, forLoop);
        assertLineColumnAre(2, 1, initClause);
        assertLineColumnAre(3, 1, condClause);
        assertLineColumnAre(4, 1, incrClause);
    }

    @Test
    public void linenoInfix() {
        AstRoot root =
                parse(
                        "\nvar d = a\n"
                                + "    + \n"
                                + "    b;\n"
                                + "var\n"
                                + "   e =\n"
                                + "    a +\n"
                                + "     c;\n"
                                + "var f = b\n"
                                + "    / c;\n");

        VariableDeclaration stmt1 = (VariableDeclaration) root.getFirstChild();
        List<VariableInitializer> vars1 = stmt1.getVariables();
        VariableInitializer var1 = vars1.get(0);
        Name firstVarName = (Name) var1.getTarget();
        InfixExpression var1Add = (InfixExpression) var1.getInitializer();

        VariableDeclaration stmt2 = (VariableDeclaration) stmt1.getNext();
        List<VariableInitializer> vars2 = stmt2.getVariables();
        VariableInitializer var2 = vars2.get(0);
        Name secondVarName = (Name) var2.getTarget();
        InfixExpression var2Add = (InfixExpression) var2.getInitializer();

        VariableDeclaration stmt3 = (VariableDeclaration) stmt2.getNext();
        List<VariableInitializer> vars3 = stmt3.getVariables();
        VariableInitializer var3 = vars3.get(0);
        Name thirdVarName = (Name) var3.getTarget();
        InfixExpression thirdVarDiv = (InfixExpression) var3.getInitializer();

        ReturnStatement returnStmt = (ReturnStatement) stmt3.getNext();

        assertLineColumnAre(1, 5, var1);
        assertLineColumnAre(1, 5, firstVarName);
        assertLineColumnAre(1, 9, var1Add);
        assertLineColumnAre(1, 9, var1Add.getLeft());
        assertLineColumnAre(3, 5, var1Add.getRight());

        // var directive with name on next line wrong --
        // should be 6.
        assertLineColumnAre(5, 4, var2);
        assertLineColumnAre(5, 4, secondVarName);
        assertLineColumnAre(6, 5, var2Add);
        assertLineColumnAre(6, 5, var2Add.getLeft());
        assertLineColumnAre(7, 6, var2Add.getRight());

        assertLineColumnAre(8, 5, var3);
        assertLineColumnAre(8, 5, thirdVarName);
        assertLineColumnAre(8, 9, thirdVarDiv);
        assertLineColumnAre(8, 9, thirdVarDiv.getLeft());
        assertLineColumnAre(9, 7, thirdVarDiv.getRight());
    }

    @Test
    public void linenoPrefix() {
        AstRoot root = parse("\na++;\n" + "   --\n" + "   b;\n");

        ExpressionStatement first = (ExpressionStatement) root.getFirstChild();
        ExpressionStatement secondStmt = (ExpressionStatement) first.getNext();
        UpdateExpression firstOp = (UpdateExpression) first.getExpression();
        UpdateExpression secondOp = (UpdateExpression) secondStmt.getExpression();
        AstNode firstVarRef = firstOp.getOperand();
        AstNode secondVarRef = secondOp.getOperand();

        assertLineColumnAre(1, 1, firstOp);
        assertLineColumnAre(2, 4, secondOp);
        assertLineColumnAre(1, 1, firstVarRef);
        assertLineColumnAre(3, 4, secondVarRef);
    }

    @Test
    public void linenoIf() {
        AstRoot root =
                parse(
                        "\nif\n"
                                + "   (a == 3)\n"
                                + "   {\n"
                                + "     b = 0;\n"
                                + "   }\n"
                                + "     else\n"
                                + "   {\n"
                                + "     c = 1;\n"
                                + "   }\n");

        IfStatement ifStmt = (IfStatement) root.getFirstChild();
        AstNode condClause = ifStmt.getCondition();
        AstNode thenClause = ifStmt.getThenPart();
        AstNode elseClause = ifStmt.getElsePart();

        assertLineColumnAre(1, 1, ifStmt);
        assertLineColumnAre(2, 5, condClause);
        assertLineColumnAre(3, 4, thenClause);
        assertLineColumnAre(7, 4, elseClause);
    }

    @Test
    public void linenoTry() {
        AstRoot root =
                parse(
                        "\ntry {\n"
                                + "    var x = 1;\n"
                                + "} catch\n"
                                + "    (err)\n"
                                + "{\n"
                                + "} finally {\n"
                                + "    var y = 2;\n"
                                + "}\n");

        TryStatement tryStmt = (TryStatement) root.getFirstChild();
        AstNode tryBlock = tryStmt.getTryBlock();
        List<CatchClause> catchBlocks = tryStmt.getCatchClauses();
        CatchClause catchClause = catchBlocks.get(0);
        Scope catchVarBlock = catchClause.getBody();
        Name catchVar = catchClause.getVarName();
        AstNode finallyBlock = tryStmt.getFinallyBlock();
        AstNode finallyStmt = (AstNode) finallyBlock.getFirstChild();

        assertLineColumnAre(1, 1, tryStmt);
        assertLineColumnAre(1, 5, tryBlock);
        assertLineColumnAre(5, 1, catchVarBlock);
        assertLineColumnAre(4, 6, catchVar);
        assertLineColumnAre(3, 3, catchClause);
        assertLineColumnAre(6, 11, finallyBlock);
        assertLineColumnAre(7, 5, finallyStmt);
    }

    @Test
    public void linenoConditional() {
        AstRoot root = parse("\na\n" + "    ?\n" + "    b\n" + "    :\n" + "    c\n" + "    ;\n");

        ExpressionStatement ex = (ExpressionStatement) root.getFirstChild();
        ConditionalExpression hook = (ConditionalExpression) ex.getExpression();
        AstNode condExpr = hook.getTestExpression();
        AstNode thenExpr = hook.getTrueExpression();
        AstNode elseExpr = hook.getFalseExpression();

        assertLineColumnAre(1, 1, hook);
        assertLineColumnAre(1, 1, condExpr);
        assertLineColumnAre(3, 5, thenExpr);
        assertLineColumnAre(5, 5, elseExpr);
    }

    @Test
    public void linenoLabel() {
        AstRoot root = parse("\nfoo:\n" + "a = 1;\n" + "bar:\n" + "b = 2;\n");

        LabeledStatement firstStmt = (LabeledStatement) root.getFirstChild();
        LabeledStatement secondStmt = (LabeledStatement) firstStmt.getNext();

        assertLineColumnAre(1, 1, firstStmt);
        assertLineColumnAre(3, 1, secondStmt);
    }

    @Test
    public void linenoCompare() {
        AstRoot root = parse("\na\n" + "<\n" + "b\n");

        ExpressionStatement expr = (ExpressionStatement) root.getFirstChild();
        InfixExpression compare = (InfixExpression) expr.getExpression();
        AstNode lhs = compare.getLeft();
        AstNode rhs = compare.getRight();

        assertLineColumnAre(1, 1, lhs);
        assertLineColumnAre(1, 1, compare);
        assertLineColumnAre(3, 1, rhs);
    }

    @Test
    public void linenoEq() {
        AstRoot root = parse("\na\n" + "==\n" + "b\n");
        ExpressionStatement expr = (ExpressionStatement) root.getFirstChild();
        InfixExpression compare = (InfixExpression) expr.getExpression();
        AstNode lhs = compare.getLeft();
        AstNode rhs = compare.getRight();

        assertLineColumnAre(1, 1, lhs);
        assertLineColumnAre(1, 1, compare);
        assertLineColumnAre(3, 1, rhs);
    }

    @Test
    public void linenoPlusEq() {
        AstRoot root = parse("\na\n" + "+=\n" + "b\n");
        ExpressionStatement expr = (ExpressionStatement) root.getFirstChild();
        Assignment assign = (Assignment) expr.getExpression();
        AstNode lhs = assign.getLeft();
        AstNode rhs = assign.getRight();

        assertLineColumnAre(1, 1, lhs);
        assertLineColumnAre(1, 1, assign);
        assertLineColumnAre(3, 1, rhs);
    }

    @Test
    public void linenoComma() {
        AstRoot root = parse("\na,\n" + "    b,\n" + "   c;\n");

        ExpressionStatement stmt = (ExpressionStatement) root.getFirstChild();
        InfixExpression comma1 = (InfixExpression) stmt.getExpression();
        InfixExpression comma2 = (InfixExpression) comma1.getLeft();
        AstNode cRef = comma1.getRight();
        AstNode aRef = comma2.getLeft();
        AstNode bRef = comma2.getRight();

        assertLineColumnAre(1, 1, comma1);
        assertLineColumnAre(1, 1, comma2);
        assertLineColumnAre(1, 1, aRef);
        assertLineColumnAre(2, 5, bRef);
        assertLineColumnAre(3, 4, cRef);
    }

    @Test
    public void regexpLocation() {
        AstNode root = parse("\nvar path =\n" + "      replace(\n" + "/a/g," + "'/');\n");

        VariableDeclaration firstVarDecl = (VariableDeclaration) root.getFirstChild();
        List<VariableInitializer> vars1 = firstVarDecl.getVariables();
        VariableInitializer firstInitializer = vars1.get(0);
        Name firstVarName = (Name) firstInitializer.getTarget();
        FunctionCall callNode = (FunctionCall) firstInitializer.getInitializer();
        AstNode fnName = callNode.getTarget();
        List<AstNode> args = callNode.getArguments();
        RegExpLiteral regexObject = (RegExpLiteral) args.get(0);
        AstNode aString = args.get(1);

        assertLineColumnAre(1, 1, firstVarDecl);
        assertLineColumnAre(1, 5, firstVarName);
        assertLineColumnAre(2, 7, callNode);
        assertLineColumnAre(2, 7, fnName);
        assertLineColumnAre(3, 1, regexObject);
        assertLineColumnAre(3, 6, aString);
    }

    @Test
    public void nestedOr() {
        AstNode root =
                parse(
                        "\nif (a && \n"
                                + "    b() || \n"
                                + "    /* comment */\n"
                                + "    c) {\n"
                                + "}\n");

        IfStatement ifStmt = (IfStatement) root.getFirstChild();
        InfixExpression orClause = (InfixExpression) ifStmt.getCondition();
        InfixExpression andClause = (InfixExpression) orClause.getLeft();
        AstNode cName = orClause.getRight();

        assertLineColumnAre(1, 1, ifStmt);
        assertLineColumnAre(1, 5, orClause);
        assertLineColumnAre(1, 5, andClause);
        assertLineColumnAre(4, 5, cName);
    }

    @Test
    public void objectLitGetterAndSetter() {
        AstNode root =
                parse(
                        "'use strict';\n"
                                + "function App() {}\n"
                                + "App.prototype = {\n"
                                + "  get appData() { return this.appData_; },\n"
                                + "  set appData(data) { this.appData_ = data; }\n"
                                + "};");
        assertNotNull(root);
    }

    @Test
    public void objectLitLocation() {
        AstNode root =
                parse(
                        "\nvar foo =\n"
                                + "{ \n"
                                + "'A' : 'A', \n"
                                + "'B' : 'B', \n"
                                + " 'C' : \n"
                                + "      'C' \n"
                                + "};\n");

        VariableDeclaration firstVarDecl = (VariableDeclaration) root.getFirstChild();
        List<VariableInitializer> vars1 = firstVarDecl.getVariables();
        VariableInitializer firstInitializer = vars1.get(0);
        Name firstVarName = (Name) firstInitializer.getTarget();

        ObjectLiteral objectLiteral = (ObjectLiteral) firstInitializer.getInitializer();
        List<ObjectProperty> props = objectLiteral.getElements();
        ObjectProperty firstObjectLit = props.get(0);
        ObjectProperty secondObjectLit = props.get(1);
        ObjectProperty thirdObjectLit = props.get(2);

        AstNode firstKey = firstObjectLit.getLeft();
        AstNode firstValue = firstObjectLit.getRight();
        AstNode secondKey = secondObjectLit.getLeft();
        AstNode secondValue = secondObjectLit.getRight();
        AstNode thirdKey = thirdObjectLit.getLeft();
        AstNode thirdValue = thirdObjectLit.getRight();

        assertLineColumnAre(1, 5, firstVarName);
        assertLineColumnAre(2, 1, objectLiteral);
        assertLineColumnAre(3, 1, firstObjectLit);
        assertLineColumnAre(3, 1, firstKey);
        assertLineColumnAre(3, 7, firstValue);

        assertLineColumnAre(4, 1, secondKey);
        assertLineColumnAre(4, 7, secondValue);

        assertLineColumnAre(5, 2, thirdKey);
        assertLineColumnAre(6, 7, thirdValue);
    }

    @Test
    public void tryWithoutCatchLocation() {
        AstNode root =
                parse("\ntry {\n" + "  var x = 1;\n" + "} finally {\n" + "  var y = 2;\n" + "}\n");

        TryStatement tryStmt = (TryStatement) root.getFirstChild();
        AstNode tryBlock = tryStmt.getTryBlock();
        List<CatchClause> catchBlocks = tryStmt.getCatchClauses();
        Scope finallyBlock = (Scope) tryStmt.getFinallyBlock();
        AstNode finallyStmt = (AstNode) finallyBlock.getFirstChild();

        assertLineColumnAre(1, 1, tryStmt);
        assertLineColumnAre(1, 5, tryBlock);
        assertLineColumnAre(3, 11, finallyBlock);
        assertLineColumnAre(4, 3, finallyStmt);
    }

    @Test
    public void tryWithoutFinallyLocation() {
        AstNode root =
                parse(
                        "\ntry {\n"
                                + "  var x = 1;\n"
                                + "} catch (ex) {\n"
                                + "  var y = 2;\n"
                                + "}\n");

        TryStatement tryStmt = (TryStatement) root.getFirstChild();
        Scope tryBlock = (Scope) tryStmt.getTryBlock();
        List<CatchClause> catchBlocks = tryStmt.getCatchClauses();
        CatchClause catchClause = catchBlocks.get(0);
        AstNode catchStmt = catchClause.getBody();
        AstNode exceptionVar = catchClause.getVarName();
        AstNode varDecl = (AstNode) catchStmt.getFirstChild();

        assertLineColumnAre(1, 1, tryStmt);
        assertLineColumnAre(1, 5, tryBlock);
        assertLineColumnAre(3, 3, catchClause);
        assertLineColumnAre(3, 14, catchStmt);
        assertLineColumnAre(3, 10, exceptionVar);
        assertLineColumnAre(4, 3, varDecl);
    }

    @Test
    public void linenoMultilineEq() {
        AstRoot root =
                parse(
                        "\nif\n"
                                + "    (((a == \n"
                                + "  3) && \n"
                                + "  (b == 2)) || \n"
                                + " (c == 1)) {\n"
                                + "}\n");
        IfStatement ifStmt = (IfStatement) root.getFirstChild();
        InfixExpression orTest = (InfixExpression) ifStmt.getCondition();
        ParenthesizedExpression cTestParen = (ParenthesizedExpression) orTest.getRight();
        InfixExpression cTest = (InfixExpression) cTestParen.getExpression();
        ParenthesizedExpression andTestParen = (ParenthesizedExpression) orTest.getLeft();
        InfixExpression andTest = (InfixExpression) andTestParen.getExpression();
        AstNode aTest = andTest.getLeft();
        AstNode bTest = andTest.getRight();

        assertLineColumnAre(1, 1, ifStmt);
        assertLineColumnAre(2, 6, orTest);
        assertLineColumnAre(2, 7, andTest);
        assertLineColumnAre(2, 7, aTest);
        assertLineColumnAre(4, 3, bTest);
        assertLineColumnAre(5, 3, cTest);
        assertLineColumnAre(5, 2, cTestParen);
        assertLineColumnAre(2, 6, andTestParen);
    }

    @Test
    public void linenoMultilineBitTest() {
        AstRoot root =
                parse(
                        "\nif (\n"
                                + "      ((a \n"
                                + "        | 3 \n"
                                + "       ) == \n"
                                + "       (b \n"
                                + "        & 2)) && \n"
                                + "      ((a \n"
                                + "         ^ 0xffff) \n"
                                + "       != \n"
                                + "       (c \n"
                                + "        << 1))) {\n"
                                + "}\n");

        IfStatement ifStmt = (IfStatement) root.getFirstChild();
        InfixExpression andTest = (InfixExpression) ifStmt.getCondition();
        ParenthesizedExpression bigLHSExpr = (ParenthesizedExpression) andTest.getLeft();
        ParenthesizedExpression bigRHSExpr = (ParenthesizedExpression) andTest.getRight();

        InfixExpression eqTest = (InfixExpression) bigLHSExpr.getExpression();
        InfixExpression notEqTest = (InfixExpression) bigRHSExpr.getExpression();

        ParenthesizedExpression test1Expr = (ParenthesizedExpression) eqTest.getLeft();
        ParenthesizedExpression test2Expr = (ParenthesizedExpression) eqTest.getRight();

        ParenthesizedExpression test3Expr = (ParenthesizedExpression) notEqTest.getLeft();
        ParenthesizedExpression test4Expr = (ParenthesizedExpression) notEqTest.getRight();

        InfixExpression bitOrTest = (InfixExpression) test1Expr.getExpression();
        InfixExpression bitAndTest = (InfixExpression) test2Expr.getExpression();
        InfixExpression bitXorTest = (InfixExpression) test3Expr.getExpression();
        InfixExpression bitShiftTest = (InfixExpression) test4Expr.getExpression();

        assertLineColumnAre(1, 1, ifStmt);

        assertLineColumnAre(2, 7, bigLHSExpr);
        assertLineColumnAre(7, 7, bigRHSExpr);
        assertLineColumnAre(2, 8, eqTest);
        assertLineColumnAre(7, 8, notEqTest);

        assertLineColumnAre(2, 8, test1Expr);
        assertLineColumnAre(5, 8, test2Expr);
        assertLineColumnAre(7, 8, test3Expr);
        assertLineColumnAre(10, 8, test4Expr);

        assertLineColumnAre(2, 9, bitOrTest);
        assertLineColumnAre(5, 9, bitAndTest);
        assertLineColumnAre(7, 9, bitXorTest);
        assertLineColumnAre(10, 9, bitShiftTest);
    }

    @Test
    public void linenoFunctionCall() {
        AstNode root = parse("\nfoo.\n" + "bar.\n" + "baz(1);");

        ExpressionStatement stmt = (ExpressionStatement) root.getFirstChild();
        FunctionCall fc = (FunctionCall) stmt.getExpression();
        // Line number should get closest to the actual paren.
        assertLineColumnAre(1, 1, fc);
    }

    @Test
    public void linenoName() {
        AstNode root = parse("\na;\n" + "b.\n" + "c;\n");

        ExpressionStatement exprStmt = (ExpressionStatement) root.getFirstChild();
        AstNode aRef = exprStmt.getExpression();
        ExpressionStatement bExprStmt = (ExpressionStatement) exprStmt.getNext();
        AstNode bRef = bExprStmt.getExpression();

        assertLineColumnAre(1, 1, aRef);
        assertLineColumnAre(2, 1, bRef);
    }

    @Test
    public void linenoDeclaration() {
        AstNode root = parse("\na.\n" + "b=\n" + "function() {};\n");

        ExpressionStatement exprStmt = (ExpressionStatement) root.getFirstChild();
        Assignment fnAssignment = (Assignment) exprStmt.getExpression();
        PropertyGet aDotbName = (PropertyGet) fnAssignment.getLeft();
        AstNode aName = aDotbName.getLeft();
        AstNode bName = aDotbName.getRight();
        FunctionNode fnNode = (FunctionNode) fnAssignment.getRight();

        assertLineColumnAre(1, 1, fnAssignment);
        assertLineColumnAre(1, 1, aDotbName);
        assertLineColumnAre(1, 1, aName);
        assertLineColumnAre(2, 1, bName);
        assertLineColumnAre(3, 1, fnNode);
    }

    @Test
    public void inOperatorInForLoop1() {
        parse("var a={};function b_(p){ return p;};" + "for(var i=b_(\"length\" in a);i<0;) {}");
    }

    @Test
    public void inOperatorInForLoop2() {
        parse("var a={}; for (;(\"length\" in a);) {}");
    }

    @Test
    public void inOperatorInForLoop3() {
        parse("for (x in y) {}");
    }

    @Test
    public void jsDocAttachment1() {
        AstRoot root = parse("/** @type number */var a;");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */", root.getComments().first().getValue());
        assertNotNull(root.getFirstChild().getNext().getJsDoc());
    }

    @Test
    public void jsDocAttachment2() {
        AstRoot root = parse("/** @type number */a.b;");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */", root.getComments().first().getValue());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild().getNext();
        assertNotNull(st.getExpression().getJsDoc());
    }

    @Test
    public void jsDocAttachment3() {
        AstRoot root = parse("var a = /** @type number */(x);");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */", root.getComments().first().getValue());
        VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
        VariableInitializer vi = vd.getVariables().get(0);
        assertNotNull(vi.getInitializer().getJsDoc());
    }

    @Test
    public void jsDocAttachment4() {
        AstRoot root = parse("(function() {/** should not be attached */})()");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        FunctionCall fc = (FunctionCall) st.getExpression();
        ParenthesizedExpression pe = (ParenthesizedExpression) fc.getTarget();
        assertNull(pe.getJsDoc());
    }

    @Test
    public void jsDocAttachment5() {
        AstRoot root = parse("({/** attach me */ 1: 2});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        NumberLiteral number = (NumberLiteral) lit.getElements().get(0).getLeft();
        assertNotNull(number.getJsDoc());
    }

    @Test
    public void jsDocAttachment6() {
        AstRoot root = parse("({1: /** don't attach me */ 2, 3: 4});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        for (ObjectProperty el : lit.getElements()) {
            assertNull(el.getLeft().getJsDoc());
            assertNull(el.getRight().getJsDoc());
        }
    }

    @Test
    public void jsDocAttachment7() {
        AstRoot root = parse("({/** attach me */ '1': 2});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        StringLiteral stringLit = (StringLiteral) lit.getElements().get(0).getLeft();
        assertNotNull(stringLit.getJsDoc());
    }

    @Test
    public void jsDocAttachment8() {
        AstRoot root = parse("({'1': /** attach me */ (foo())});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        ParenthesizedExpression parens =
                (ParenthesizedExpression) lit.getElements().get(0).getRight();
        assertNotNull(parens.getJsDoc());
    }

    @Test
    public void jsDocAttachment9() {
        AstRoot root = parse("({/** attach me */ foo: 2});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        Name objLitKey = (Name) lit.getElements().get(0).getLeft();
        assertNotNull(objLitKey.getJsDoc());
    }

    @Test
    public void jsDocAttachment10() {
        AstRoot root = parse("({foo: /** attach me */ (bar)});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        ParenthesizedExpression parens =
                (ParenthesizedExpression) lit.getElements().get(0).getRight();
        assertNotNull(parens.getJsDoc());
    }

    @Test
    public void jsDocAttachment11() {
        AstRoot root = parse("({/** attach me */ get foo() {}});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        Name objLitKey = (Name) lit.getElements().get(0).getLeft();
        assertNotNull(objLitKey.getJsDoc());
    }

    @Test
    public void jsDocAttachment12() {
        AstRoot root = parse("({/** attach me */ get 1() {}});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        NumberLiteral number = (NumberLiteral) lit.getElements().get(0).getLeft();
        assertNotNull(number.getJsDoc());
    }

    @Test
    public void jsDocAttachment13() {
        AstRoot root = parse("({/** attach me */ get 'foo'() {}});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
        ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
        StringLiteral stringLit = (StringLiteral) lit.getElements().get(0).getLeft();
        assertNotNull(stringLit.getJsDoc());
    }

    @Test
    public void jsDocAttachment14() {
        AstRoot root = parse("var a = (/** @type {!Foo} */ {});");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type {!Foo} */", root.getComments().first().getValue());
        VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
        VariableInitializer vi = vd.getVariables().get(0);
        assertNotNull(((ParenthesizedExpression) vi.getInitializer()).getExpression().getJsDoc());
    }

    @Test
    public void jsDocAttachment15() {
        AstRoot root = parse("/** @private */ x(); function f() {}");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());

        ExpressionStatement st = (ExpressionStatement) root.getFirstChild().getNext();
        assertNotNull(st.getExpression().getJsDoc());
    }

    @Test
    public void jsDocAttachment16() {
        AstRoot root =
                parse(
                        "/** @suppress {with} */ with (context) {\n"
                                + "  eval('[' + expr + ']');\n"
                                + "}\n");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());

        WithStatement st = (WithStatement) root.getFirstChild().getNext();
        assertNotNull(st.getJsDoc());
    }

    @Test
    public void jsDocAttachment17() {
        AstRoot root = parse("try { throw 'a'; } catch (/** @type {string} */ e) {\n" + "}\n");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());

        TryStatement tryNode = (TryStatement) root.getFirstChild();
        CatchClause catchNode = tryNode.getCatchClauses().get(0);
        assertNotNull(catchNode.getVarName().getJsDoc());
    }

    @Test
    public void jsDocAttachment18() {
        AstRoot root = parse("function f(/** @type {string} */ e) {}\n");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());

        FunctionNode function = (FunctionNode) root.getFirstChild();
        AstNode param = function.getParams().get(0);
        assertNotNull(param.getJsDoc());
    }

    @Test
    public void parsingWithoutJSDoc() {
        AstRoot root = parse("var a = /** @type number */(x);", false);
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */", root.getComments().first().getValue());
        VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
        VariableInitializer vi = vd.getVariables().get(0);
        assertTrue(vi.getInitializer() instanceof ParenthesizedExpression);
    }

    @Test
    public void parseCommentsAsReader() throws IOException {
        AstRoot root = parseAsReader("/** a */var a;\n /** b */var b; /** c */var c;");
        assertNotNull(root.getComments());
        assertEquals(3, root.getComments().size());
        Comment[] comments = new Comment[3];
        comments = root.getComments().toArray(comments);
        assertEquals("/** a */", comments[0].getValue());
        assertEquals("/** b */", comments[1].getValue());
        assertEquals("/** c */", comments[2].getValue());
    }

    @Test
    public void parseCommentsAsReader2() throws IOException {
        String js = "";
        for (int i = 0; i < 100; i++) {
            String stri = Integer.toString(i);
            js += "/** Some comment for a" + stri + " */" + "var a" + stri + " = " + stri + ";\n";
        }
        AstRoot root = parseAsReader(js);
    }

    @Test
    public void linenoCommentsWithJSDoc() throws IOException {
        AstRoot root =
                parseAsReader(
                        "/* foo \n"
                                + " bar \n"
                                + "*/\n"
                                + "/** @param {string} x */\n"
                                + "function a(x) {};\n");
        assertNotNull(root.getComments());
        assertEquals(2, root.getComments().size());
        Comment[] comments = new Comment[2];
        comments = root.getComments().toArray(comments);
        assertLineColumnAre(0, 1, comments[0]);
        assertLineColumnAre(3, 1, comments[1]);
    }

    @Test
    public void parseUnicodeFormatStringLiteral() {
        AstRoot root = parse("'A\u200DB'");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        StringLiteral stringLit = (StringLiteral) st.getExpression();
        assertEquals("A\u200DB", stringLit.getValue());
    }

    @Test
    public void parseUnicodeFormatName() {
        AstRoot root = parse("A\u200DB");
        AstNode first = ((ExpressionStatement) root.getFirstChild()).getExpression();
        assertEquals("AB", first.getString());
    }

    @Test
    public void parseUnicodeMultibyteCharacter() {
        AstRoot root = parse("\uD842\uDFB7");
        AstNode first = ((ExpressionStatement) root.getFirstChild()).getExpression();
        assertEquals("𠮷", first.getString());
    }

    @Test
    public void parseMultibyteCharacter_StringLiteral() {
        AstRoot root = parse("'\uD83C\uDF1F'");
        StringLiteral first =
                (StringLiteral) ((ExpressionStatement) root.getFirstChild()).getExpression();
        assertEquals(4, first.getLength());
        assertEquals("'🌟'", first.getValue(true));
    }

    @Test
    public void parseMultibyteCharacter_TemplateLiteral() {
        AstRoot root = parse("`\uD83C\uDF1F`");
        TemplateLiteral first =
                (TemplateLiteral) ((ExpressionStatement) root.getFirstChild()).getExpression();
        TemplateCharacters templateCharacter = (TemplateCharacters) first.getElement(0);
        assertEquals(2, templateCharacter.getLength());
        assertEquals("🌟", templateCharacter.getValue());
        assertEquals(4, first.getLength());
    }

    @Test
    public void parseMultibyteCharacter_XMLLiteral() {
        AstRoot root = parse("<xml>\uD83C\uDF1F</xml>");
        XmlLiteral first =
                (XmlLiteral) ((ExpressionStatement) root.getFirstChild()).getExpression();
        XmlFragment fragment = first.getFragments().get(0);
        assertEquals(13, fragment.getLength());
        assertEquals("<xml>🌟</xml>", fragment.toSource());
    }

    @Test
    public void parseMultibyteCharacter_Comment() {
        AstRoot root = parse("/*\uD83C\uDF1F*/");
        Comment comment = root.getComments().first();
        assertEquals(6, comment.getLength());
        assertEquals("/*🌟*/", comment.getValue());
    }

    @Test
    public void parseUnicodeIdentifierPartWhichIsNotJavaIdentifierPart() {
        // On the JDK 11 I'm using, Character.isUnicodeIdentifierPart(U+9FEB) returns true
        // but Character.isJavaIdentifierPart(U+9FEB) returns false. On a JDK 17 results
        // seem to vary, but I think it's enough to verify that TokenStream uses
        // the unicode methods and not the java methods.
        AstRoot root = parse("a\u9FEB");
        AstNode first = ((ExpressionStatement) root.getFirstChild()).getExpression();
        assertEquals("a鿫", first.getString());
    }

    @Test
    public void parseUnicodeReservedKeywords1() {
        AstRoot root = parse("\\u0069\\u0066");
        AstNode first = ((ExpressionStatement) root.getFirstChild()).getExpression();
        assertEquals("i\\u0066", first.getString());
    }

    @Test
    public void parseUnicodeReservedKeywords2() {
        AstRoot root = parse("v\\u0061\\u0072");
        AstNode first = ((ExpressionStatement) root.getFirstChild()).getExpression();
        assertEquals("va\\u0072", first.getString());
    }

    @Test
    public void parseUnicodeReservedKeywords3() {
        // All are keyword "while"
        AstRoot root =
                parse(
                        "w\\u0068\\u0069\\u006C\\u0065;"
                                + "\\u0077\\u0068il\\u0065; \\u0077h\\u0069le;");
        AstNode first = ((ExpressionStatement) root.getFirstChild()).getExpression();
        AstNode second = ((ExpressionStatement) root.getFirstChild().getNext()).getExpression();
        AstNode third =
                ((ExpressionStatement) root.getFirstChild().getNext().getNext()).getExpression();
        assertEquals("whil\\u0065", first.getString());
        assertEquals("whil\\u0065", second.getString());
        assertEquals("whil\\u0065", third.getString());
    }

    @Test
    public void parseObjectLiteral1() {
        environment.setReservedKeywordAsIdentifier(true);

        parse("({a:1});");
        parse("({'a':1});");
        parse("({0:1});");

        // property getter and setter definitions accept string and number
        parse("({get a() {return 1}});");
        parse("({get 'a'() {return 1}});");
        parse("({get 0() {return 1}});");

        parse("({set a(a) {return 1}});");
        parse("({set 'a'(a) {return 1}});");
        parse("({set 0(a) {return 1}});");

        // keywords ok
        parse("({function:1});");
        // reserved words ok
        parse("({float:1});");
    }

    @Test
    public void parseObjectLiteral2() {
        // keywords, fail
        environment.setReservedKeywordAsIdentifier(false);
        expectParseErrors("({function:1});", new String[] {"invalid property id"});

        environment.setReservedKeywordAsIdentifier(true);

        // keywords ok
        parse("({function:1});");
    }

    @Test
    public void parseObjectLiteral3() {
        environment.setLanguageVersion(Context.VERSION_1_8);
        environment.setReservedKeywordAsIdentifier(true);
        parse("var {get} = {get:1};");

        environment.setReservedKeywordAsIdentifier(false);
        parse("var {get} = {get:1};");
        expectParseErrors("var {get} = {if:1};", new String[] {"invalid property id"});
    }

    @Test
    public void parseKeywordPropertyAccess() {
        environment.setReservedKeywordAsIdentifier(true);

        // keywords ok
        parse("({function:1}).function;");

        // reserved words ok.
        parse("({import:1}).import;");
    }

    @Test
    public void throwStatement() {
        environment.setStrictMode(true);
        parse(
                "function A(a) { switch (a) { default: throw \"some error\" } }",
                null,
                new String[] {"missing ; after statement"},
                true);
    }

    @Test
    public void parseErrorRecovery() {
        expectErrorWithRecovery(")", 1);
    }

    @Test
    public void parseErrorRecovery2() {
        expectErrorWithRecovery("print('Hi');)foo('bar');Silly", 2);
    }

    @Test
    public void parseErrorRecovery3() {
        expectErrorWithRecovery(")))", 5);
    }

    @Test
    public void identifierIsReservedWordMessage() {
        environment.setReservedKeywordAsIdentifier(false);
        expectParseErrors(
                "interface: while (true){ }",
                new String[] {"identifier is a reserved word: interface"});
    }

    // Check that error recovery is working by returning a parsing exception, but only
    // when thrown by runtimeError. This is testing a regression in which the error recovery in
    // certain cases would trigger an infinite loop. We do this by counting the number
    // of parsing errors that are expected.
    private void expectErrorWithRecovery(String code, int maxErrors) {
        environment.setRecoverFromErrors(true);
        environment.setErrorReporter(
                new ErrorReporter() {
                    private int errorCount = 0;

                    @Override
                    public void warning(String msg, String name, int line, String str, int col) {
                        throw new AssertionError("Not expecting a warning");
                    }

                    @Override
                    public EvaluatorException runtimeError(
                            String msg, String name, int line, String str, int col) {
                        return new EvaluatorException(msg, name, line, str, col);
                    }

                    @Override
                    public void error(String msg, String name, int line, String str, int col) {
                        assertTrue(++errorCount <= maxErrors);
                    }
                });

        Parser p = new Parser(environment);
        try {
            p.parse(code, code, 0);
            assertFalse("Expected an EvaluatorException", true);
        } catch (EvaluatorException ee) {
            // Normal failure
        }
    }

    @Test
    public void reportError() {
        expectParseErrors(
                "'use strict';(function(eval) {})();",
                new String[] {"\"eval\" is not a valid identifier for this use in strict mode."});
    }

    @Test
    public void basicFunction() {
        AstNode root = parse("function f() { return 1; }");
        FunctionNode f = (FunctionNode) root.getFirstChild();
        assertEquals("f", f.getName());
        assertFalse(f.isGenerator());
        assertFalse(f.isES6Generator());
    }

    @Test
    public void es6Generator() {
        environment.setLanguageVersion(Context.VERSION_ES6);
        AstNode root = parse("function * g() { return true; }");
        FunctionNode f = (FunctionNode) root.getFirstChild();
        assertEquals("g", f.getName());
        assertTrue(f.isGenerator());
        assertTrue(f.isES6Generator());
    }

    @Test
    public void memberFunctionGenerator() {
        environment.setLanguageVersion(Context.VERSION_ES6);
        AstNode root = parse("o = { *g() { return true; } }");
        ExpressionStatement expr = (ExpressionStatement) root.getFirstChild();
        assertTrue(expr.getExpression() instanceof Assignment);
        assertTrue(((Assignment) expr.getExpression()).getRight() instanceof ObjectLiteral);
        ObjectLiteral obj = (ObjectLiteral) ((Assignment) expr.getExpression()).getRight();
        assertEquals(1, obj.getElements().size());
        ObjectProperty g = obj.getElements().get(0);

        assertTrue(g.getLeft() instanceof GeneratorMethodDefinition);
        assertLineColumnAre(0, 7, g.getLeft());
        AstNode genMethodName = ((GeneratorMethodDefinition) g.getLeft()).getMethodName();
        assertTrue(genMethodName instanceof Name);
        assertLineColumnAre(0, 8, genMethodName);

        assertTrue(g.getRight() instanceof FunctionNode);
        assertTrue(((FunctionNode) g.getRight()).isES6Generator());
    }

    @Test
    public void es6GeneratorNot() {
        expectParseErrors(
                "function * notES6() { return true; }",
                new String[] {"missing ( before function parameters."});
    }

    @Test
    public void oomOnInvalidInput() {
        expectParseErrors("`\\u{8", new String[] {"syntax error"});
    }

    private void expectParseErrors(String string, String[] errors) {
        parse(string, errors, null, false);
    }

    private AstRoot parse(String string) {
        return parse(string, true);
    }

    private AstRoot parse(String string, boolean jsdoc) {
        return parse(string, null, null, jsdoc);
    }

    private AstRoot parse(
            String string, final String[] errors, final String[] warnings, boolean jsdoc) {
        return parse(string, errors, warnings, jsdoc, environment);
    }

    static AstRoot parse(
            String string,
            final String[] errors,
            final String[] warnings,
            boolean jsdoc,
            CompilerEnvirons env) {
        TestErrorReporter testErrorReporter =
                new TestErrorReporter(errors, warnings) {
                    @Override
                    public EvaluatorException runtimeError(
                            String message,
                            String sourceName,
                            int line,
                            String lineSource,
                            int lineOffset) {
                        if (errors == null) {
                            throw new UnsupportedOperationException();
                        }
                        return new EvaluatorException(
                                message, sourceName, line, lineSource, lineOffset);
                    }
                };
        env.setErrorReporter(testErrorReporter);

        env.setRecordingComments(true);
        env.setRecordingLocalJsDocComments(jsdoc);

        Parser p = new Parser(env, testErrorReporter);
        AstRoot script = null;
        try {
            script = p.parse(string, null, 0);
        } catch (EvaluatorException e) {
            if (errors == null) {
                // EvaluationExceptions should not occur when we aren't expecting
                // errors.
                throw e;
            }
        }

        assertTrue(testErrorReporter.hasEncounteredAllErrors());
        assertTrue(testErrorReporter.hasEncounteredAllWarnings());

        return script;
    }

    private AstRoot parseAsReader(String string) throws IOException {
        TestErrorReporter testErrorReporter = new TestErrorReporter(null, null);
        environment.setErrorReporter(testErrorReporter);

        environment.setRecordingComments(true);
        environment.setRecordingLocalJsDocComments(true);

        Parser p = new Parser(environment, testErrorReporter);
        AstRoot script = p.parse(string, null, 0);

        assertTrue(testErrorReporter.hasEncounteredAllErrors());
        assertTrue(testErrorReporter.hasEncounteredAllWarnings());

        return script;
    }

    private void assertLineColumnAre(int line, int column, Node node) {
        Assertions.assertEquals(line, node.getLineno(), "line number mismatch");
        Assertions.assertEquals(column, node.getColumn(), "column number mismatch");
    }
}
