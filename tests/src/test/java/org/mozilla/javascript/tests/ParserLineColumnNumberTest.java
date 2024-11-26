package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.BigIntLiteral;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ComputedPropertyKey;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
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
import org.mozilla.javascript.ast.TaggedTemplateLiteral;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.UpdateExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;
import org.mozilla.javascript.ast.Yield;

class ParserLineColumnNumberTest {
    CompilerEnvirons environment;

    @BeforeEach
    public void setUp() throws Exception {
        environment = new CompilerEnvirons();
    }

    @Test
    void declarationVar() {
        AstRoot root = myParse("var a =\n1;");
        VariableDeclaration varStatement =
                assertInstanceOf(VariableDeclaration.class, root.getFirstChild());
        assertLineColumnAre(varStatement, 0, 1);
        assertEquals(1, varStatement.getVariables().size());
        VariableInitializer varInitializer = varStatement.getVariables().get(0);
        assertLineColumnAre(varInitializer.getTarget(), 0, 5);
        assertLineColumnAre(varInitializer.getInitializer(), 1, 1);
    }

    @Test
    void declarationConst() {
        AstRoot root = myParse("const a =\n1;");
        VariableDeclaration varStatement =
                assertInstanceOf(VariableDeclaration.class, root.getFirstChild());
        assertLineColumnAre(varStatement, 0, 1);
        assertEquals(1, varStatement.getVariables().size());
        VariableInitializer varInitializer = varStatement.getVariables().get(0);
        assertLineColumnAre(varInitializer.getTarget(), 0, 7);
        assertLineColumnAre(varInitializer.getInitializer(), 1, 1);
    }

    @Test
    void declarationLet() {
        AstRoot root = myParse("let a =\n1;");
        VariableDeclaration varStatement =
                assertInstanceOf(VariableDeclaration.class, root.getFirstChild());
        assertLineColumnAre(varStatement, 0, 1);
        assertEquals(1, varStatement.getVariables().size());
        VariableInitializer varInitializer = varStatement.getVariables().get(0);
        assertLineColumnAre(varInitializer.getTarget(), 0, 5);
        assertLineColumnAre(varInitializer.getInitializer(), 1, 1);
    }

    @Test
    void controlFlowIf() {
        AstRoot root = myParse("if (a) {\n" + "  b\n" + "} else {\n" + "  c;\n" + "}");
        IfStatement ifStatement = assertInstanceOf(IfStatement.class, root.getFirstChild());
        assertLineColumnAre(ifStatement, 0, 1);
        assertLineColumnAre(ifStatement.getCondition(), 0, 5);

        Scope thenPart = assertInstanceOf(Scope.class, ifStatement.getThenPart());
        assertLineColumnAre(thenPart, 0, 8);
        assertLineColumnAre(thenPart.getStatements().get(0), 1, 3);

        Scope elsePart = assertInstanceOf(Scope.class, ifStatement.getElsePart());
        assertLineColumnAre(elsePart, 2, 8);
        assertLineColumnAre(elsePart.getStatements().get(0), 3, 3);
    }

    @Test
    void controlFlowSwitch() {
        AstRoot root =
                myParse(
                        "switch (a) {\n"
                                + "  case 1: break;\n"
                                + "  case 2: {\n"
                                + "    f();\n"
                                + "  }\n"
                                + "  default:\n"
                                + "    g();\n"
                                + "}");
        SwitchStatement switchStatement =
                assertInstanceOf(SwitchStatement.class, root.getFirstChild());
        assertLineColumnAre(switchStatement, 0, 1);
        assertLineColumnAre(switchStatement.getExpression(), 0, 9);
        assertEquals(3, switchStatement.getCases().size());

        SwitchCase case1 = switchStatement.getCases().get(0);
        assertLineColumnAre(case1, 1, 3);
        assertLineColumnAre(case1.getExpression(), 1, 8);
        assertInstanceOf(BreakStatement.class, case1.getStatements().get(0));
        assertLineColumnAre(case1.getStatements().get(0), 1, 11);

        SwitchCase case2 = switchStatement.getCases().get(1);
        assertLineColumnAre(case2, 2, 3);
        assertLineColumnAre(case2.getExpression(), 2, 8);
        assertInstanceOf(Scope.class, case2.getStatements().get(0));
        assertLineColumnAre(case2.getStatements().get(0), 2, 11);

        SwitchCase caseDefault = switchStatement.getCases().get(2);
        assertLineColumnAre(caseDefault, 5, 3);
        assertTrue(caseDefault.isDefault());
        assertNull(caseDefault.getExpression());
    }

    @Test
    void controlFlowDo() {
        AstRoot root = myParse("" + "do {\n" + "  break;\n" + "} while (cond)");
        DoLoop doStatement = assertInstanceOf(DoLoop.class, root.getFirstChild());
        assertLineColumnAre(doStatement, 0, 1);

        Scope body = assertInstanceOf(Scope.class, doStatement.getBody());
        assertLineColumnAre(body, 0, 4);

        assertEquals(1, body.getStatements().size());
        BreakStatement breakStatement =
                assertInstanceOf(BreakStatement.class, body.getStatements().get(0));
        assertLineColumnAre(breakStatement, 1, 3);

        assertLineColumnAre(doStatement.getCondition(), 2, 10);
    }

    @Test
    void controlFlowWhile() {
        AstRoot root = myParse("" + "while (cond) {\n" + "  continue;\n" + "}");
        WhileLoop whileStatement = assertInstanceOf(WhileLoop.class, root.getFirstChild());
        assertLineColumnAre(whileStatement, 0, 1);
        assertLineColumnAre(whileStatement.getCondition(), 0, 8);

        Scope body = assertInstanceOf(Scope.class, whileStatement.getBody());
        assertLineColumnAre(body, 0, 14);

        assertEquals(1, body.getStatements().size());
        ContinueStatement continueStatement =
                assertInstanceOf(ContinueStatement.class, body.getStatements().get(0));
        assertLineColumnAre(continueStatement, 1, 3);
    }

    @Test
    void controlFlowFor() {
        AstRoot root = myParse("" + "for (var i = 0; i < 10; ++i) {\n" + "}");
        ForLoop forStatement = assertInstanceOf(ForLoop.class, root.getFirstChild());
        assertLineColumnAre(forStatement, 0, 1);
        assertLineColumnAre(forStatement.getInitializer(), 0, 6);
        assertLineColumnAre(forStatement.getCondition(), 0, 17);
        assertLineColumnAre(forStatement.getIncrement(), 0, 25);
        Scope body = assertInstanceOf(Scope.class, forStatement.getBody());
        assertLineColumnAre(body, 0, 30);
    }

    @Test
    void controlFlowForIn() {
        AstRoot root = myParse("" + "for (let key in obj) {\n" + "}");
        ForInLoop forStatement = assertInstanceOf(ForInLoop.class, root.getFirstChild());
        assertFalse(forStatement.isForOf());
        assertLineColumnAre(forStatement, 0, 1);
        assertLineColumnAre(forStatement.getIterator(), 0, 6);
        assertLineColumnAre(forStatement.getIteratedObject(), 0, 17);
        Scope body = assertInstanceOf(Scope.class, forStatement.getBody());
        assertLineColumnAre(body, 0, 22);
    }

    @Test
    void controlFlowForOf() {
        AstRoot root = myParse("" + "for (var key of obj) {\n" + "}");
        ForInLoop forStatement = assertInstanceOf(ForInLoop.class, root.getFirstChild());
        assertTrue(forStatement.isForOf());
        assertLineColumnAre(forStatement, 0, 1);
        assertLineColumnAre(forStatement.getIterator(), 0, 6);
        assertLineColumnAre(forStatement.getIteratedObject(), 0, 17);
        Scope body = assertInstanceOf(Scope.class, forStatement.getBody());
        assertLineColumnAre(body, 0, 22);
    }

    @Test
    void controlFlowTryCatchFinally() {
        AstRoot root =
                myParse(
                        ""
                                + "try {\n"
                                + "  f();\n"
                                + "} catch (\n"
                                + "  err) {\n"
                                + "    g(err);\n"
                                + "} finally {\n"
                                + "  i();\n"
                                + "}\n");
        TryStatement tryStatement = assertInstanceOf(TryStatement.class, root.getFirstChild());
        assertLineColumnAre(tryStatement, 0, 1);
        Scope tryBlock = assertInstanceOf(Scope.class, tryStatement.getTryBlock());
        assertLineColumnAre(tryBlock, 0, 5);

        assertEquals(1, tryStatement.getCatchClauses().size());
        CatchClause catchClause = tryStatement.getCatchClauses().get(0);
        assertLineColumnAre(catchClause, 2, 3);
        assertLineColumnAre(catchClause.getVarName(), 3, 3);
        assertLineColumnAre(catchClause.getBody(), 3, 8);

        assertNotNull(tryStatement.getFinallyBlock());
        assertLineColumnAre(tryStatement.getFinallyBlock(), 5, 11);
    }

    @Test
    void controlFlowTryCatchNoVar() {
        AstRoot root =
                myParse(
                        ""
                                + "try {\n"
                                + "  throw /* comment */ 'err';\n"
                                + "} catch {\n"
                                + "  g(err);\n"
                                + "}\n");

        TryStatement tryStatement = assertInstanceOf(TryStatement.class, root.getFirstChild());
        assertLineColumnAre(tryStatement, 0, 1);
        Scope tryBlock = assertInstanceOf(Scope.class, tryStatement.getTryBlock());
        assertLineColumnAre(tryBlock, 0, 5);
        assertEquals(1, tryBlock.getStatements().size());
        ThrowStatement throwStatement =
                assertInstanceOf(ThrowStatement.class, tryBlock.getStatements().get(0));
        assertLineColumnAre(throwStatement, 1, 3);
        assertNotNull(throwStatement.getExpression());
        assertLineColumnAre(throwStatement.getExpression(), 1, 23);

        assertEquals(1, tryStatement.getCatchClauses().size());
        CatchClause catchClause = tryStatement.getCatchClauses().get(0);
        assertLineColumnAre(catchClause, 2, 3);
        assertNull(catchClause.getVarName());
        assertLineColumnAre(catchClause.getBody(), 2, 9);

        assertNull(tryStatement.getFinallyBlock());
    }

    @Test
    void controlFlowLabels() {
        AstRoot root =
                myParse(
                        ""
                                + "l1: while (true) {\n"
                                + "  l2: for (var i in obj)\n"
                                + "    break l2;\n"
                                + "  continue l1;\n"
                                + "}\n");

        LabeledStatement l1Statement =
                assertInstanceOf(LabeledStatement.class, root.getFirstChild());
        assertLineColumnAre(l1Statement, 0, 1);
        assertEquals(1, l1Statement.getLabels().size());
        assertLineColumnAre(l1Statement.getLabels().get(0), 0, 1);

        WhileLoop whileLoop = assertInstanceOf(WhileLoop.class, l1Statement.getStatement());
        assertLineColumnAre(whileLoop, 0, 5);
        Scope whileBody = assertInstanceOf(Scope.class, whileLoop.getBody());
        assertLineColumnAre(whileBody, 0, 18);

        assertEquals(2, whileBody.getStatements().size());
        LabeledStatement l2Statement =
                assertInstanceOf(LabeledStatement.class, whileBody.getStatements().get(0));
        assertLineColumnAre(l2Statement, 1, 3);
        assertEquals(1, l2Statement.getLabels().size());
        assertLineColumnAre(l2Statement.getLabels().get(0), 1, 3);

        ForInLoop forLoop = assertInstanceOf(ForInLoop.class, l2Statement.getStatement());
        assertLineColumnAre(forLoop, 1, 7);

        BreakStatement breakStatement = assertInstanceOf(BreakStatement.class, forLoop.getBody());
        assertLineColumnAre(breakStatement, 2, 5);
        assertNotNull(breakStatement.getBreakLabel());
        assertLineColumnAre(breakStatement.getBreakLabel(), 2, 11);

        ContinueStatement continueStatement =
                assertInstanceOf(ContinueStatement.class, whileBody.getStatements().get(1));
        assertLineColumnAre(continueStatement, 3, 3);
        assertNotNull(continueStatement.getLabel());
        assertLineColumnAre(continueStatement.getLabel(), 3, 12);
    }

    @Test
    void statementWith() {
        AstRoot root = myParse(" with (a) {\n" + "}\n");

        WithStatement withStatement = assertInstanceOf(WithStatement.class, root.getFirstChild());
        assertLineColumnAre(withStatement, 0, 2);
        assertLineColumnAre(withStatement.getExpression(), 0, 8);
        Scope block = assertInstanceOf(Scope.class, withStatement.getStatement());
        assertLineColumnAre(block, 0, 11);
    }

    @Test
    void statementDebugger() {
        AstRoot root = myParse(" debugger");

        KeywordLiteral debuggerStatement =
                assertInstanceOf(KeywordLiteral.class, root.getFirstChild());
        assertLineColumnAre(debuggerStatement, 0, 2);
    }

    @Test
    void statementEmpty() {
        AstRoot root = myParse(" ;");

        EmptyStatement emptyStatement =
                assertInstanceOf(EmptyStatement.class, root.getFirstChild());
        assertLineColumnAre(emptyStatement, 0, 2);
    }

    @Test
    void functionDeclarationNoArgs() {
        AstRoot root = myParse("" + "function f() {\n" + "  return a();\n" + "}");

        FunctionNode functionDecl = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(functionDecl, 0, 1);
        assertNotNull(functionDecl.getFunctionName());
        assertLineColumnAre(functionDecl.getFunctionName(), 0, 10);
        Block body = assertInstanceOf(Block.class, functionDecl.getBody());
        assertLineColumnAre(body, 0, 14);
        ReturnStatement returnStatement =
                assertInstanceOf(ReturnStatement.class, body.getFirstChild());
        assertLineColumnAre(returnStatement, 1, 3);
        assertNotNull(returnStatement.getReturnValue());
        FunctionCall returnValue =
                assertInstanceOf(FunctionCall.class, returnStatement.getReturnValue());
        assertLineColumnAre(returnValue, 1, 10);
    }

    @Test
    void functionDeclarationWithArgs() {
        AstRoot root = myParse(" function /*comment*/ \nf( a, /* comment */ b) {}");

        FunctionNode functionDecl = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(functionDecl, 0, 2);
        assertNotNull(functionDecl.getFunctionName());
        assertLineColumnAre(functionDecl.getFunctionName(), 1, 1);
        Block body = assertInstanceOf(Block.class, functionDecl.getBody());
        assertLineColumnAre(body, 1, 24);

        assertEquals(2, functionDecl.getParams().size());
        Name firstParam = assertInstanceOf(Name.class, functionDecl.getParams().get(0));
        assertLineColumnAre(firstParam, 1, 4);
        Name secondParam = assertInstanceOf(Name.class, functionDecl.getParams().get(1));
        assertLineColumnAre(secondParam, 1, 21);
    }

    @Test
    void functionDeclarationDirectives() {
        AstRoot root = myParse("function a() {\n" + "  'use strict';\n" + "  return 42;\n" + "}");
        FunctionNode fun = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(fun, 0, 1);
        Block aBody = assertInstanceOf(Block.class, fun.getBody());
        assertLineColumnAre(aBody, 0, 14);
        ExpressionStatement statement =
                assertInstanceOf(ExpressionStatement.class, aBody.getFirstChild());
        assertLineColumnAre(statement, 1, 3);
        StringLiteral directive = assertInstanceOf(StringLiteral.class, statement.getExpression());
        assertLineColumnAre(directive, 1, 3);
        ReturnStatement returnStatement =
                assertInstanceOf(ReturnStatement.class, statement.getNext());
        assertLineColumnAre(returnStatement, 2, 3);
    }

    @Test
    void functionDeclarationRest() {
        AstRoot root = myParse("function b(... /* a comment */ rest) {}");
        FunctionNode fun = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(fun, 0, 1);
        assertEquals(1, fun.getParams().size());
        Name restParam = assertInstanceOf(Name.class, fun.getParams().get(0));
        assertLineColumnAre(restParam, 0, 12); // Start of the dots
    }

    @Test
    void functionDeclarationDefaultArgs() {
        AstRoot root = myParse("function b(a  =42) {}");
        FunctionNode fun = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(fun, 0, 1);
        assertEquals(1, fun.getParams().size());
        Name paramA = assertInstanceOf(Name.class, fun.getParams().get(0));
        assertLineColumnAre(paramA, 0, 12);
        assertEquals(2, fun.getDefaultParams().size());
        assertEquals("a", fun.getDefaultParams().get(0));
        NumberLiteral paramADefaultValue =
                assertInstanceOf(NumberLiteral.class, fun.getDefaultParams().get(1));
        assertLineColumnAre(paramADefaultValue, 0, 16);
    }

    @Test
    void functionDeclarationDestructuring() {
        AstRoot root = myParse("function f({props}) {}");
        FunctionNode fun = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(fun, 0, 1);
        assertEquals(1, fun.getParams().size());
        ObjectLiteral param = assertInstanceOf(ObjectLiteral.class, fun.getParams().get(0));
        assertLineColumnAre(param, 0, 12);
    }

    @Test
    void generatorDeclaration() {
        AstRoot root = myParse("" + " function* f() {\n" + "  yield 1;\n" + "  return a;\n" + "}");
        FunctionNode generatorDecl = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(generatorDecl, 0, 2);
        assertTrue(generatorDecl.isGenerator());
        assertTrue(generatorDecl.isES6Generator());
        assertNotNull(generatorDecl.getFunctionName());
        assertLineColumnAre(generatorDecl.getFunctionName(), 0, 12);
        Block body = assertInstanceOf(Block.class, generatorDecl.getBody());
        assertLineColumnAre(body, 0, 16);

        ExpressionStatement firstStatement =
                assertInstanceOf(ExpressionStatement.class, body.getFirstChild());
        assertLineColumnAre(firstStatement, 1, 3);
        Yield yield = assertInstanceOf(Yield.class, firstStatement.getExpression());
        assertLineColumnAre(yield, 1, 3);
        assertNotNull(yield.getValue());
        assertLineColumnAre(yield.getValue(), 1, 9);

        ReturnStatement returnStatement =
                assertInstanceOf(ReturnStatement.class, firstStatement.getNext());
        assertLineColumnAre(returnStatement, 2, 3);
        assertNotNull(returnStatement.getReturnValue());
        assertLineColumnAre(returnStatement.getReturnValue(), 2, 10);
    }

    @Test
    void expressionArrowFunction() {
        AstRoot root = myParse(" (a=1) /* comment */ =>\n3;");
        ExpressionStatement expressionStatement =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expressionStatement, 0, 2);
        FunctionNode arrowFunction =
                assertInstanceOf(FunctionNode.class, expressionStatement.getExpression());
        assertLineColumnAre(arrowFunction, 0, 2);

        assertEquals(1, arrowFunction.getParams().size());
        Name firstParam = assertInstanceOf(Name.class, arrowFunction.getParams().get(0));
        assertLineColumnAre(firstParam, 0, 3);
        assertEquals(2, arrowFunction.getDefaultParams().size());
        assertEquals("a", arrowFunction.getDefaultParams().get(0));
        NumberLiteral firstParamDefaultValue =
                assertInstanceOf(NumberLiteral.class, arrowFunction.getDefaultParams().get(1));
        assertLineColumnAre(firstParamDefaultValue, 0, 5);

        Block body = assertInstanceOf(Block.class, arrowFunction.getBody());
        assertLineColumnAre(body, 0, 22);
        ReturnStatement statement = assertInstanceOf(ReturnStatement.class, body.getFirstChild());
        assertLineColumnAre(statement, 1, 1);
    }

    @Test
    void expressionName() {
        AstRoot root = myParse(" a");
        ExpressionStatement nameExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(nameExpr, 0, 2);
        Name name = assertInstanceOf(Name.class, nameExpr.getExpression());
        assertLineColumnAre(name, 0, 2);
    }

    @Test
    void expressionNameInBlock() {
        AstRoot root = myParse(" {a}");
        Scope block = assertInstanceOf(Scope.class, root.getFirstChild());
        assertLineColumnAre(block, 0, 2);
        assertEquals(1, block.getStatements().size());
        AstNode firstStatement = block.getStatements().get(0);
        assertLineColumnAre(firstStatement, 0, 3);
    }

    @Test
    void expressionNumericLiteral() {
        AstRoot root = myParse(" 42");
        ExpressionStatement expr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expr, 0, 2);
        NumberLiteral name = assertInstanceOf(NumberLiteral.class, expr.getExpression());
        assertLineColumnAre(name, 0, 2);
    }

    @Test
    void expressionBooleanLiteral() {
        AstRoot root = myParse(" true");
        ExpressionStatement expr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expr, 0, 2);
        KeywordLiteral name = assertInstanceOf(KeywordLiteral.class, expr.getExpression());
        assertLineColumnAre(name, 0, 2);
    }

    @Test
    void expressionStringLiteral() {
        AstRoot root = myParse(" 'x'");
        ExpressionStatement expr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expr, 0, 2);
        StringLiteral name = assertInstanceOf(StringLiteral.class, expr.getExpression());
        assertLineColumnAre(name, 0, 2);
    }

    @Test
    void expressionBigIntLiteral() {
        AstRoot root = myParse(" 42n");
        ExpressionStatement expr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expr, 0, 2);
        BigIntLiteral name = assertInstanceOf(BigIntLiteral.class, expr.getExpression());
        assertLineColumnAre(name, 0, 2);
    }

    @Test
    void expressionRegexpLiteral() {
        AstRoot root = myParse("/* c */ /regex/y ");
        Comment comment = assertInstanceOf(Comment.class, root.getFirstChild());
        assertLineColumnAre(comment, 0, 1);
        ExpressionStatement statement =
                assertInstanceOf(ExpressionStatement.class, comment.getNext());
        assertLineColumnAre(statement, 0, 9);
        RegExpLiteral infix = assertInstanceOf(RegExpLiteral.class, statement.getExpression());
        assertLineColumnAre(infix, 0, 9);
    }

    @Test
    void expressionUnaryIncrementPrefix() {
        AstRoot root = myParse(" ++a;");
        ExpressionStatement expr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expr, 0, 2);
        UpdateExpression increment = assertInstanceOf(UpdateExpression.class, expr.getExpression());
        assertLineColumnAre(increment, 0, 2);
        assertTrue(increment.isPrefix());
        assertFalse(increment.isPostfix());
        Name name = assertInstanceOf(Name.class, increment.getOperand());
        assertLineColumnAre(name, 0, 4);
    }

    @Test
    void expressionUnaryIncrementSuffix() {
        AstRoot root = myParse(" a++;");
        ExpressionStatement expr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expr, 0, 2);
        UpdateExpression increment = assertInstanceOf(UpdateExpression.class, expr.getExpression());
        assertLineColumnAre(increment, 0, 2);
        assertFalse(increment.isPrefix());
        assertTrue(increment.isPostfix());
        Name name = assertInstanceOf(Name.class, increment.getOperand());
        assertLineColumnAre(name, 0, 2);
    }

    @Test
    void expressionUnaryNot() {
        AstRoot root = myParse(" !a;");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        UnaryExpression unaryExpr =
                assertInstanceOf(UnaryExpression.class, firstExpr.getExpression());
        assertLineColumnAre(unaryExpr, 0, 2);
        Name name = assertInstanceOf(Name.class, unaryExpr.getOperand());
        assertLineColumnAre(name, 0, 3);
    }

    @Test
    void expressionUnaryPlus() {
        AstRoot root = myParse(" +  a;");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        UnaryExpression unaryExpr =
                assertInstanceOf(UnaryExpression.class, firstExpr.getExpression());
        assertLineColumnAre(unaryExpr, 0, 2);
        Name name = assertInstanceOf(Name.class, unaryExpr.getOperand());
        assertLineColumnAre(name, 0, 5);
    }

    @Test
    void expressionUnaryMinus() {
        AstRoot root = myParse(" - a;");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        UnaryExpression unaryExpr =
                assertInstanceOf(UnaryExpression.class, firstExpr.getExpression());
        assertLineColumnAre(unaryExpr, 0, 2);
        Name name = assertInstanceOf(Name.class, unaryExpr.getOperand());
        assertLineColumnAre(name, 0, 4);
    }

    @Test
    void expressionDeleteName() {
        AstRoot root = myParse(" delete   a;");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        UnaryExpression unaryExpr =
                assertInstanceOf(UnaryExpression.class, firstExpr.getExpression());
        assertLineColumnAre(unaryExpr, 0, 2);
        Name name = assertInstanceOf(Name.class, unaryExpr.getOperand());
        assertLineColumnAre(name, 0, 11);
    }

    @Test
    void expressionFunctionCallNoArg() {
        AstRoot root = myParse(" a ()");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        FunctionCall functionCall = assertInstanceOf(FunctionCall.class, firstExpr.getExpression());
        assertLineColumnAre(functionCall, 0, 2);
        Name name = assertInstanceOf(Name.class, functionCall.getTarget());
        assertLineColumnAre(name, 0, 2);
        assertTrue(functionCall.getArguments().isEmpty());
    }

    @Test
    void expressionFunctionCallWithArgs() {
        AstRoot root = myParse("a(b,  42)");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 1);
        FunctionCall functionCall = assertInstanceOf(FunctionCall.class, firstExpr.getExpression());
        assertLineColumnAre(functionCall, 0, 1);
        Name name = assertInstanceOf(Name.class, functionCall.getTarget());
        assertLineColumnAre(name, 0, 1);

        assertEquals(2, functionCall.getArguments().size());
        Name firstArg = assertInstanceOf(Name.class, functionCall.getArguments().get(0));
        assertLineColumnAre(firstArg, 0, 3);
        NumberLiteral secondArg =
                assertInstanceOf(NumberLiteral.class, functionCall.getArguments().get(1));
        assertLineColumnAre(secondArg, 0, 7);
    }

    @Test
    void expressionIffe() {
        AstRoot root = myParse(" (function() {})()");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        FunctionCall functionCall = assertInstanceOf(FunctionCall.class, firstExpr.getExpression());
        assertLineColumnAre(functionCall, 0, 2);
        ParenthesizedExpression name =
                assertInstanceOf(ParenthesizedExpression.class, functionCall.getTarget());
        assertLineColumnAre(name, 0, 2);
    }

    @Test
    void expressionMemberCall() {
        AstRoot root = myParse(" a /* comment */. b ()");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        FunctionCall functionCall = assertInstanceOf(FunctionCall.class, firstExpr.getExpression());
        assertLineColumnAre(functionCall, 0, 2);
        PropertyGet propGet = assertInstanceOf(PropertyGet.class, functionCall.getTarget());
        assertLineColumnAre(propGet, 0, 2);
        Name target = assertInstanceOf(Name.class, propGet.getTarget());
        assertLineColumnAre(target, 0, 2);
        Name property = assertInstanceOf(Name.class, propGet.getProperty());
        assertLineColumnAre(property, 0, 19);
    }

    @Test
    void expressionCallAfterCall() {
        AstRoot root = myParse("a.b().c()");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 1);
        FunctionCall callC = assertInstanceOf(FunctionCall.class, firstExpr.getExpression());
        assertLineColumnAre(callC, 0, 1);
        PropertyGet getC = assertInstanceOf(PropertyGet.class, callC.getTarget());
        assertLineColumnAre(callC, 0, 1);
        Name nameC = assertInstanceOf(Name.class, getC.getProperty());
        assertLineColumnAre(nameC, 0, 7);
        FunctionCall callB = assertInstanceOf(FunctionCall.class, getC.getTarget());
        assertLineColumnAre(callB, 0, 1);
        PropertyGet propGet = assertInstanceOf(PropertyGet.class, callB.getTarget());
        assertLineColumnAre(propGet, 0, 1);
        Name nameB = assertInstanceOf(Name.class, propGet.getProperty());
        assertLineColumnAre(nameB, 0, 3);
        Name nameA = assertInstanceOf(Name.class, propGet.getTarget());
        assertLineColumnAre(nameA, 0, 1);
    }

    @Test
    void expressionOptionalCall() {
        AstRoot root = myParse(" a?.(x)");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        FunctionCall functionCall = assertInstanceOf(FunctionCall.class, firstExpr.getExpression());
        assertLineColumnAre(functionCall, 0, 2);
        Name name = assertInstanceOf(Name.class, functionCall.getTarget());
        assertLineColumnAre(name, 0, 2);
        assertEquals(1, functionCall.getArguments().size());
        Name property = assertInstanceOf(Name.class, functionCall.getArguments().get(0));
        assertLineColumnAre(property, 0, 6);
    }

    @Test
    void expressionGeneratorThrow() {
        AstRoot root = myParse(" a = gen();\n a.throw()");
        ExpressionStatement statement1 =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(statement1, 0, 2);

        ExpressionStatement statement2 =
                assertInstanceOf(ExpressionStatement.class, statement1.getNext());
        assertLineColumnAre(statement2, 1, 2);
        FunctionCall functionCall =
                assertInstanceOf(FunctionCall.class, statement2.getExpression());
        assertLineColumnAre(functionCall, 1, 2);
        PropertyGet propertyGet = assertInstanceOf(PropertyGet.class, functionCall.getTarget());
        assertLineColumnAre(propertyGet, 1, 2);
        assertLineColumnAre(propertyGet.getTarget(), 1, 2);
        assertLineColumnAre(propertyGet.getProperty(), 1, 4);
    }

    @Test
    void expressionPropertyElem() {
        AstRoot root = myParse(" a[ 1]");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        ElementGet propGet = assertInstanceOf(ElementGet.class, firstExpr.getExpression());
        assertLineColumnAre(propGet, 0, 2);
        Name target = assertInstanceOf(Name.class, propGet.getTarget());
        assertLineColumnAre(target, 0, 2);
        NumberLiteral property = assertInstanceOf(NumberLiteral.class, propGet.getElement());
        assertLineColumnAre(property, 0, 5);
    }

    @Test
    void expressionPropertyElemOptional() {
        AstRoot root = myParse(" a?.[ 1]");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        ElementGet propGet = assertInstanceOf(ElementGet.class, firstExpr.getExpression());
        assertLineColumnAre(propGet, 0, 2);
        Name target = assertInstanceOf(Name.class, propGet.getTarget());
        assertLineColumnAre(target, 0, 2);
        NumberLiteral property = assertInstanceOf(NumberLiteral.class, propGet.getElement());
        assertLineColumnAre(property, 0, 7);
    }

    @Test
    void expressionInfixOperator() {
        AstRoot root = myParse(" a + /*comment*/ 1");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        InfixExpression infix = assertInstanceOf(InfixExpression.class, firstExpr.getExpression());
        assertLineColumnAre(infix, 0, 2);
        Name left = assertInstanceOf(Name.class, infix.getLeft());
        assertLineColumnAre(left, 0, 2);
        NumberLiteral right = assertInstanceOf(NumberLiteral.class, infix.getRight());
        assertLineColumnAre(right, 0, 18);
    }

    @Test
    void statementAssignment() {
        AstRoot root = myParse(" a |= 1");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        Assignment assignment = assertInstanceOf(Assignment.class, firstExpr.getExpression());
        assertLineColumnAre(assignment, 0, 2);
        Name target = assertInstanceOf(Name.class, assignment.getLeft());
        assertLineColumnAre(target, 0, 2);
        NumberLiteral right = assertInstanceOf(NumberLiteral.class, assignment.getRight());
        assertLineColumnAre(right, 0, 7);
    }

    @Test
    void expressionHook() {
        AstRoot root = myParse(" a ? b : 1");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        ConditionalExpression hook =
                assertInstanceOf(ConditionalExpression.class, firstExpr.getExpression());
        assertLineColumnAre(hook, 0, 2);
        Name testExpression = assertInstanceOf(Name.class, hook.getTestExpression());
        assertLineColumnAre(testExpression, 0, 2);
        Name trueExpression = assertInstanceOf(Name.class, hook.getTrueExpression());
        assertLineColumnAre(trueExpression, 0, 6);
        NumberLiteral falseExpression =
                assertInstanceOf(NumberLiteral.class, hook.getFalseExpression());
        assertLineColumnAre(falseExpression, 0, 10);
    }

    @Test
    void expressionParenthesis() {
        AstRoot root = myParse(" (3)");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        ParenthesizedExpression parenthesizedExpression =
                assertInstanceOf(ParenthesizedExpression.class, firstExpr.getExpression());
        assertLineColumnAre(parenthesizedExpression, 0, 2);
        NumberLiteral nestedExpression =
                assertInstanceOf(NumberLiteral.class, parenthesizedExpression.getExpression());
        assertLineColumnAre(nestedExpression, 0, 3);
    }

    @Test
    void expressionComma() {
        AstRoot root = myParse(" a, /*comment*/ 3");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        InfixExpression infixExpression =
                assertInstanceOf(InfixExpression.class, firstExpr.getExpression());
        assertLineColumnAre(infixExpression, 0, 2);
        Name left = assertInstanceOf(Name.class, infixExpression.getLeft());
        assertLineColumnAre(left, 0, 2);
        NumberLiteral right = assertInstanceOf(NumberLiteral.class, infixExpression.getRight());
        assertLineColumnAre(right, 0, 17);
    }

    @Test
    void expressionNew() {
        AstRoot root = myParse(" new  Foo( a)");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        NewExpression newExpression =
                assertInstanceOf(NewExpression.class, firstExpr.getExpression());
        assertLineColumnAre(newExpression, 0, 2);
        Name target = assertInstanceOf(Name.class, newExpression.getTarget());
        assertLineColumnAre(target, 0, 7);
        assertEquals(1, newExpression.getArguments().size());
        Name arg = assertInstanceOf(Name.class, newExpression.getArguments().get(0));
        assertLineColumnAre(arg, 0, 12);
    }

    @Test
    void expressionObjectLiteral() {
        AstRoot root =
                myParse(
                        " o = {a: 1,\n"
                                + " [b]: 2,\n"
                                + " c() { return 3; },\n"
                                + " d: function() { return 4; },\n"
                                + " get e() { return 5; },\n"
                                + " set f(value) { this._f = value; }\n"
                                + "}");
        ExpressionStatement expressionStatement =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(expressionStatement, 0, 2);
        Assignment assignment =
                assertInstanceOf(Assignment.class, expressionStatement.getExpression());
        assertLineColumnAre(assignment, 0, 2);
        ObjectLiteral objectLiteral = assertInstanceOf(ObjectLiteral.class, assignment.getRight());
        assertLineColumnAre(objectLiteral, 0, 6);
        assertEquals(6, objectLiteral.getElements().size());

        ObjectProperty propA =
                assertInstanceOf(ObjectProperty.class, objectLiteral.getElements().get(0));
        assertLineColumnAre(propA, 0, 7);
        assertLineColumnAre(propA.getLeft(), 0, 7);
        assertLineColumnAre(propA.getRight(), 0, 10);

        ObjectProperty propB =
                assertInstanceOf(ObjectProperty.class, objectLiteral.getElements().get(1));
        assertLineColumnAre(propB, 1, 2);
        ComputedPropertyKey propBKey = assertInstanceOf(ComputedPropertyKey.class, propB.getLeft());
        assertLineColumnAre(propBKey, 1, 2);
        assertLineColumnAre(propBKey.getExpression(), 1, 3);
        assertLineColumnAre(propB.getRight(), 1, 7);

        ObjectProperty propC =
                assertInstanceOf(ObjectProperty.class, objectLiteral.getElements().get(2));
        assertLineColumnAre(propC, 2, 2);
        assertLineColumnAre(propC.getLeft(), 2, 2);
        assertLineColumnAre(propC.getRight(), 2, 2);

        ObjectProperty propD =
                assertInstanceOf(ObjectProperty.class, objectLiteral.getElements().get(3));
        assertLineColumnAre(propD, 3, 2);
        assertLineColumnAre(propD.getLeft(), 3, 2);
        assertLineColumnAre(propD.getRight(), 3, 5);

        ObjectProperty propE =
                assertInstanceOf(ObjectProperty.class, objectLiteral.getElements().get(4));
        assertLineColumnAre(propE, 4, 2);
        assertLineColumnAre(propE.getLeft(), 4, 2);
        assertLineColumnAre(propE.getRight(), 4, 6);

        ObjectProperty propF =
                assertInstanceOf(ObjectProperty.class, objectLiteral.getElements().get(5));
        assertLineColumnAre(propF, 5, 2);
        assertLineColumnAre(propF.getLeft(), 5, 2);
        assertLineColumnAre(propF.getRight(), 5, 6);
    }

    @Test
    void expressionArrayLiteral() {
        AstRoot root = myParse(" [a, /*comment*/ 3]");
        ExpressionStatement firstExpr =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(firstExpr, 0, 2);
        ArrayLiteral arrayLiteral = assertInstanceOf(ArrayLiteral.class, firstExpr.getExpression());
        assertLineColumnAre(arrayLiteral, 0, 2);

        assertEquals(2, arrayLiteral.getElements().size());
        Name elem0 = assertInstanceOf(Name.class, arrayLiteral.getElements().get(0));
        assertLineColumnAre(elem0, 0, 3);
        NumberLiteral elem1 =
                assertInstanceOf(NumberLiteral.class, arrayLiteral.getElements().get(1));
        assertLineColumnAre(elem1, 0, 18);
    }

    @Test
    void expressionDestructuring() {
        AstRoot root = myParse(" let {a, b} = c();");
        VariableDeclaration variableDeclaration =
                assertInstanceOf(VariableDeclaration.class, root.getFirstChild());
        assertLineColumnAre(variableDeclaration, 0, 2);
        assertEquals(1, variableDeclaration.getVariables().size());

        VariableInitializer firstVar = variableDeclaration.getVariables().get(0);
        ObjectLiteral left = assertInstanceOf(ObjectLiteral.class, firstVar.getTarget());
        assertLineColumnAre(left, 0, 6);
        assertEquals(2, left.getElements().size());
        ObjectProperty firstProp =
                assertInstanceOf(ObjectProperty.class, left.getElements().get(0));
        assertLineColumnAre(firstProp, 0, 7);
        ObjectProperty secondProp =
                assertInstanceOf(ObjectProperty.class, left.getElements().get(1));
        assertLineColumnAre(secondProp, 0, 10);

        FunctionCall right = assertInstanceOf(FunctionCall.class, firstVar.getInitializer());
        assertLineColumnAre(right, 0, 15);
    }

    @Test
    void expressionTaggedTemplateLiteral() {
        AstRoot root = myParse(" f`x`");
        ExpressionStatement statement =
                assertInstanceOf(ExpressionStatement.class, root.getFirstChild());
        assertLineColumnAre(statement, 0, 2);
        TaggedTemplateLiteral taggedTemplateLiteral =
                assertInstanceOf(TaggedTemplateLiteral.class, statement.getExpression());
        assertLineColumnAre(taggedTemplateLiteral, 0, 2);
        assertLineColumnAre(taggedTemplateLiteral.getTarget(), 0, 2);
        assertLineColumnAre(taggedTemplateLiteral.getTemplateLiteral(), 0, 3);
    }

    @Test
    void commentsAndNestedFunctions() {
        AstRoot root =
                myParse(
                        " function a() {\n"
                                + "  // comment 1\n"
                                + "   // comment 2\n"
                                + "  function b() {}\n"
                                + "   /* another"
                                + "comment */"
                                + "}");
        FunctionNode funA = assertInstanceOf(FunctionNode.class, root.getFirstChild());
        assertLineColumnAre(funA, 0, 2);
        Block aBody = assertInstanceOf(Block.class, funA.getBody());
        assertLineColumnAre(aBody, 0, 15);
        Comment comment1 = assertInstanceOf(Comment.class, aBody.getFirstChild());
        assertLineColumnAre(comment1, 1, 3);
        Comment comment2 = assertInstanceOf(Comment.class, comment1.getNext());
        assertLineColumnAre(comment2, 2, 4);
        FunctionNode funB = assertInstanceOf(FunctionNode.class, comment2.getNext());
        assertLineColumnAre(funB, 3, 3);
        Comment comment3 = assertInstanceOf(Comment.class, funB.getNext());
        assertLineColumnAre(comment3, 4, 4);
    }

    @Test
    void emptySource() {
        AstRoot root = myParse("");
        assertNull(root.getFirstChild());
    }

    private AstRoot myParse(String source) {
        environment.setLanguageVersion(Context.VERSION_ES6);
        return ParserTest.parse(source, null, null, true, environment);
    }

    private void assertLineColumnAre(Node node, int line, int column) {
        assertEquals(line, node.getLineno(), "line number mismatch");
        assertEquals(column, node.getColumn(), "column number mismatch");
    }
}
