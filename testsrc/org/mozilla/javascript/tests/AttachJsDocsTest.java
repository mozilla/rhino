/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;

/**
 * Tests for attaching jsdoc comments to the AST.
 * @author Dimitris Vardoulakis
 */
public class AttachJsDocsTest extends TestCase {
  CompilerEnvirons environment;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    environment = new CompilerEnvirons();
    environment.setRecordingComments(true);
    environment.setRecordingLocalJsDocComments(true);
  }

  private AstRoot parse(String code) {
    Parser p = new Parser(environment);
    AstRoot root = p.parse(code, null, 0);
    return root;
  }

  public void testAdd() {
    AstRoot root = parse("1 + /** attach */ value;");
    ExpressionStatement estm = (ExpressionStatement) root.getFirstChild();
    InfixExpression ie = (InfixExpression) estm.getExpression();
    assertNotNull(ie.getRight().getJsDoc());
  }

  public void testArrayLit() {
    AstRoot root = parse("[1, /** attach */ 2]");
    ArrayLiteral lit = (ArrayLiteral)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(lit.getElement(1).getJsDoc());
  }

  public void testAssign1() {
    AstRoot root = parse("x = 1; /** attach */ y = 2;");
    ExpressionStatement estm = (ExpressionStatement) root.getLastChild();
    assertNotNull(estm.getExpression().getJsDoc());
  }

  public void testAssign2() {
    AstRoot root = parse("x = 1; /** attach */y.p = 2;");
    ExpressionStatement estm = (ExpressionStatement) root.getLastChild();
    assertNotNull(estm.getExpression().getJsDoc());
  }

  public void testAssign3() {
    AstRoot root =
        parse("/** @const */ var g = {}; /** @type {number} */ (g.foo) = 3;");
    ExpressionStatement estm = (ExpressionStatement) root.getLastChild();
    InfixExpression ie = (InfixExpression) estm.getExpression();
    assertNotNull(ie.getLeft().getJsDoc());
  }

  public void testBlock1() {
    AstRoot root = parse("if (x) { /** attach */ x; }");
    IfStatement ifstm = (IfStatement) root.getFirstChild();
    ExpressionStatement estm =
        (ExpressionStatement) ifstm.getThenPart().getFirstChild();
    assertNotNull(estm.getExpression().getJsDoc());
  }

  public void testBlock2() {
    AstRoot root = parse("if (x) { x; /** attach */ y; }");
    IfStatement ifstm = (IfStatement) root.getFirstChild();
    ExpressionStatement estm =
        (ExpressionStatement) ifstm.getThenPart().getLastChild();
    assertNotNull(estm.getExpression().getJsDoc());
  }

  public void testBreak() {
    AstRoot root = parse("FOO: for (;;) { break /** don't attach */ FOO; }");
    LabeledStatement ls = (LabeledStatement) root.getFirstChild();
    ForLoop fl = (ForLoop) ls.getStatement();
    BreakStatement bs = (BreakStatement) fl.getBody().getFirstChild();
    assertNull(bs.getJsDoc());
    assertNull(bs.getBreakLabel().getJsDoc());
  }

  public void testCall1() {
    AstRoot root = parse("foo/** don't attach */(1, 2);");
    FunctionCall call = (FunctionCall)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNull(call.getArguments().get(0).getJsDoc());
  }

  public void testCall2() {
    AstRoot root = parse("foo(/** attach */ 1, 2);");
    FunctionCall call = (FunctionCall)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(call.getArguments().get(0).getJsDoc());
  }

  public void testCall3() {
    // Incorrect attachment b/c the parser doesn't preserve comma positions.
    // TODO(dimvar): if this case comes up often, modify the parser to
    // remember comma positions for function decls and calls and fix the bug.
    AstRoot root = parse("foo(1 /** attach to 2nd parameter */, 2);");
    FunctionCall call = (FunctionCall)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNull(call.getArguments().get(0).getJsDoc());
    assertNotNull(call.getArguments().get(1).getJsDoc());
  }

  public void testCall4() {
    AstRoot root = parse("foo(1, 2 /** don't attach */);");
    FunctionCall call = (FunctionCall)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNull(call.getArguments().get(1).getJsDoc());
  }

  public void testCall5() {
    AstRoot root = parse("/** attach */ x(); function f() {}");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    assertNotNull(st.getExpression().getJsDoc());
  }

  public void testCall6() {
    AstRoot root = parse("(function f() { /** attach */ var x = 1; })();");
    FunctionCall call = (FunctionCall)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    ParenthesizedExpression pe = (ParenthesizedExpression) call.getTarget();
    FunctionNode target = (FunctionNode) pe.getExpression();
    assertNotNull(target.getBody().getFirstChild().getJsDoc());
  }

  public void testCall7() {
    AstRoot root = parse("/** attach */ obj.prop();");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    assertNotNull(st.getExpression().getJsDoc());
  }

  public void testCall8() {
    AstRoot root = parse("/** attach */ (obj).prop();");
    FunctionCall call = (FunctionCall)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    PropertyGet pget = (PropertyGet) call.getTarget();
    assertNotNull(pget.getTarget().getJsDoc());
  }

  public void testComma1() {
    AstRoot root = parse("(/** attach */ x, y, z);");
    AstNode paren =
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    InfixExpression comma = (InfixExpression)
        ((ParenthesizedExpression) paren).getExpression();
    InfixExpression comma2 = (InfixExpression) comma.getLeft();
    assertNotNull(comma2.getLeft().getJsDoc());
  }

  public void testComma2() {
    AstRoot root = parse("(x /** don't attach */, y, z);");
    AstNode paren =
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    InfixExpression comma = (InfixExpression)
        ((ParenthesizedExpression) paren).getExpression();
    InfixExpression comma2 = (InfixExpression) comma.getLeft();
    assertNull(comma2.getLeft().getJsDoc());
    assertNull(comma2.getRight().getJsDoc());
  }

  public void testComma3() {
    AstRoot root = parse("(x, y, /** attach */ z);");
    AstNode paren =
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    InfixExpression comma = (InfixExpression)
        ((ParenthesizedExpression) paren).getExpression();
    assertNotNull(comma.getRight().getJsDoc());
  }

  public void testContinue() {
    AstRoot root = parse("FOO: for (;;) { continue /** don't attach */ FOO; }");
    LabeledStatement ls = (LabeledStatement) root.getFirstChild();
    ForLoop fl = (ForLoop) ls.getStatement();
    ContinueStatement cont = (ContinueStatement) fl.getBody().getFirstChild();
    assertNull(cont.getJsDoc());
    assertNull(cont.getLabel().getJsDoc());
  }

  public void testDoLoop1() {
    AstRoot root = parse("do /** don't attach */ {} while (x);");
    DoLoop dl = (DoLoop) root.getFirstChild();
    assertNull(dl.getBody().getJsDoc());
  }

  public void testDoLoop2() {
    AstRoot root = parse("do {} /** don't attach */ while (x);");
    DoLoop dl = (DoLoop) root.getFirstChild();
    assertNull(dl.getCondition().getJsDoc());
  }

  public void testDot() {
    AstRoot root = parse("/** attach */a.b;");
    assertNotNull(root.getComments());
    assertEquals(1, root.getComments().size());
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    assertNotNull(st.getExpression().getJsDoc());
  }

  public void testForInLoop1() {
    AstRoot root = parse("for /** don't attach */ (var p in {}) {}");
    ForInLoop fil = (ForInLoop) root.getFirstChild();
    assertNull(fil.getJsDoc());
    assertNull(fil.getIterator().getJsDoc());
    assertNull(fil.getIteratedObject().getJsDoc());
    assertNull(fil.getBody().getJsDoc());
  }

  public void testForInLoop2() {
    AstRoot root = parse("for (/** attach */ var p in {}) {}");
    ForInLoop fil = (ForInLoop) root.getFirstChild();
    assertNull(fil.getJsDoc());
    assertNotNull(fil.getIterator().getJsDoc());
  }

  public void testForInLoop3() {
    AstRoot root = parse("for (var p in /** attach */ {}) {}");
    ForInLoop fil = (ForInLoop) root.getFirstChild();
    assertNull(fil.getJsDoc());
    assertNotNull(fil.getIteratedObject().getJsDoc());
  }

  public void testForInLoop4() {
    AstRoot root = parse("for (var p in {}) /** don't attach */ {}");
    ForInLoop fil = (ForInLoop) root.getFirstChild();
    assertNull(fil.getJsDoc());
    assertNull(fil.getBody().getJsDoc());
  }

  public void testForInLoop5() {
    AstRoot root = parse("for (var p /** don't attach */ in {}) {}");
    ForInLoop fil = (ForInLoop) root.getFirstChild();
    assertNull(fil.getJsDoc());
    assertNull(fil.getIterator().getJsDoc());
    assertNull(fil.getIteratedObject().getJsDoc());
    assertNull(fil.getBody().getJsDoc());
  }

  public void testForInLoop6() {
    AstRoot root = parse("for (var p in {} /** don't attach */) {}");
    ForInLoop fil = (ForInLoop) root.getFirstChild();
    assertNull(fil.getJsDoc());
    assertNull(fil.getIterator().getJsDoc());
    assertNull(fil.getIteratedObject().getJsDoc());
    assertNull(fil.getBody().getJsDoc());
  }

  public void testForLoop1() {
    AstRoot root = parse("for /** don't attach */ (i = 0; i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    assertNull(fl.getInitializer().getJsDoc());
    assertNull(fl.getCondition().getJsDoc());
    assertNull(fl.getIncrement().getJsDoc());
    assertNull(fl.getBody().getJsDoc());
  }

  public void testForLoop2() {
    AstRoot root = parse("for (/** attach */ i = 0; i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    assertNotNull(fl.getInitializer().getJsDoc());
  }

  public void testForLoop3() {
    AstRoot root = parse("for (i /** don't attach */ = 0; i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    InfixExpression ie = (InfixExpression) fl.getInitializer();
    assertNull(ie.getLeft().getJsDoc());
    assertNull(ie.getRight().getJsDoc());
    assertNull(fl.getCondition().getJsDoc());
  }

  public void testForLoop4() {
    AstRoot root = parse("for (i = /** attach */ 0; i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    InfixExpression ie = (InfixExpression) fl.getInitializer();
    assertNotNull(ie.getRight().getJsDoc());
  }

  public void testForLoop5() {
    AstRoot root = parse("for (i = 0 /** don't attach */; i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    InfixExpression ie = (InfixExpression) fl.getInitializer();
    assertNull(ie.getRight().getJsDoc());
    assertNull(fl.getCondition().getJsDoc());
  }

  public void testForLoop6() {
    AstRoot root = parse("for (i = 0; /** attach */ i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    InfixExpression ie = (InfixExpression) fl.getCondition();
    assertNotNull(ie.getLeft().getJsDoc());
  }

  public void testForLoop7() {
    AstRoot root = parse("for (i = 0; i < /** attach */ 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    InfixExpression ie = (InfixExpression) fl.getCondition();
    assertNotNull(ie.getRight().getJsDoc());
  }

  public void testForLoop8() {
    AstRoot root = parse("for (i = 0; i < 5; /** attach */ i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    assertNotNull(fl.getIncrement().getJsDoc());
  }

  public void testForLoop9() {
    AstRoot root = parse("for (i = 0; i < 5; i++ /** don't attach */) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    assertNull(fl.getIncrement().getJsDoc());
    assertNull(fl.getBody().getJsDoc());
  }

  public void testForLoop10() {
    AstRoot root = parse("for (i = 0; i < 5; i++) /** don't attach */ {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    assertNull(fl.getBody().getJsDoc());
  }

  public void testForLoop11() {
    AstRoot root = parse("for (/** attach */ var i = 0; i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    assertNotNull(fl.getInitializer().getJsDoc());
  }

  public void testForLoop12() {
    AstRoot root = parse("for (var i = 0 /** dont attach */; i < 5; i++) {}");
    ForLoop fl = (ForLoop) root.getFirstChild();
    assertNull(fl.getJsDoc());
    assertNull(fl.getInitializer().getJsDoc());
    assertNull(fl.getCondition().getJsDoc());
  }

  public void testFun1() {
    AstRoot root = parse("function f(/** string */ e) {}");
    FunctionNode function = (FunctionNode) root.getFirstChild();
    AstNode param = function.getParams().get(0);
    assertNotNull(param.getJsDoc());
  }

  public void testFun2() {
    AstRoot root = parse("(function() {/** don't attach */})()");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    FunctionCall fc = (FunctionCall) st.getExpression();
    assertNull(fc.getTarget().getJsDoc());
  }

  public void testFun3() {
    AstRoot root = parse("function /** string */ f (e) {}");
    FunctionNode function = (FunctionNode) root.getFirstChild();
    AstNode fname = function.getFunctionName();
    assertNotNull(fname.getJsDoc());
  }

  public void testFun4() {
    AstRoot root = parse("f = /** attach */ function(e) {};");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    InfixExpression ie = (InfixExpression) st.getExpression();
    assertNotNull(ie.getRight().getJsDoc());
  }

  public void testFun5() {
    AstRoot root = parse("x = 1; /** attach */ function f(e) {}");
    assertNotNull(root.getLastChild().getJsDoc());
  }

  public void testFun6() {
    AstRoot root = parse("function f() { /** attach */ function Foo(){} }");
    FunctionNode function = (FunctionNode) root.getFirstChild();
    assertNotNull(function.getBody().getFirstChild().getJsDoc());
  }

  public void testFun7() {
    AstRoot root = parse("(function f() { /** attach */function Foo(){} })();");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    FunctionCall call = (FunctionCall) st.getExpression();
    ParenthesizedExpression pe = (ParenthesizedExpression) call.getTarget();
    FunctionNode function = (FunctionNode) pe.getExpression();
    assertNotNull(function.getBody().getFirstChild().getJsDoc());
  }

  public void testGetElem1() {
    AstRoot root = parse("(/** attach */ {})['prop'];");
    ElementGet elm = (ElementGet)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    ParenthesizedExpression left = (ParenthesizedExpression) elm.getTarget();
    assertNotNull(left.getExpression().getJsDoc());
  }

  public void testGetElem2() {
    AstRoot root = parse("({} /** don't attach */)['prop'];");
    ElementGet elm = (ElementGet)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    ParenthesizedExpression left = (ParenthesizedExpression) elm.getTarget();
    assertNull(left.getExpression().getJsDoc());
    assertNull(elm.getElement().getJsDoc());
  }

  public void testGetElem3() {
    AstRoot root = parse("({})[/** attach */ 'prop'];");
    ElementGet elm = (ElementGet)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(elm.getElement().getJsDoc());
  }

  public void testGetProp1() {
    AstRoot root = parse("(/** attach */ {}).prop;");
    PropertyGet pget = (PropertyGet)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    ParenthesizedExpression left = (ParenthesizedExpression) pget.getTarget();
    assertNotNull(left.getExpression().getJsDoc());
  }

  public void testGetProp2() {
    AstRoot root = parse("/** attach */ ({}).prop;");
    PropertyGet pget = (PropertyGet)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(pget.getTarget().getJsDoc());
  }

  public void testGetProp3() {
    AstRoot root = parse("/** attach */ obj.prop;");
    PropertyGet pget = (PropertyGet)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(pget.getJsDoc());
  }

  public void testGetter1() {
    AstRoot root = parse("({/** attach */ get foo() {}});");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
    ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
    assertNotNull(lit.getElements().get(0).getLeft().getJsDoc());
  }

  public void testGetter2() {
    AstRoot root = parse("({/** attach */ get 1() {}});");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
    ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
    assertNotNull(lit.getElements().get(0).getLeft().getJsDoc());
  }

  public void testGetter3() {
    AstRoot root = parse("({/** attach */ get 'foo'() {}});");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
    ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
    assertNotNull(lit.getElements().get(0).getLeft().getJsDoc());
  }

  public void testHook1() {
    AstRoot root = parse("/** attach */ true ? 1 : 2;");
    ConditionalExpression hook = (ConditionalExpression)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(hook.getTestExpression().getJsDoc());
  }

  public void testHook2() {
    AstRoot root = parse("true /** don't attach */ ? 1 : 2;");
    ConditionalExpression hook = (ConditionalExpression)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNull(hook.getTestExpression().getJsDoc());
    assertNull(hook.getTrueExpression().getJsDoc());
  }

  public void testHook3() {
    AstRoot root = parse("true ? /** attach */ 1 : 2;");
    ConditionalExpression hook = (ConditionalExpression)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(hook.getTrueExpression().getJsDoc());
  }

  public void testHook4() {
    AstRoot root = parse("true ? 1 /** don't attach */ : 2;");
    ConditionalExpression hook = (ConditionalExpression)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNull(hook.getTrueExpression().getJsDoc());
    assertNull(hook.getFalseExpression().getJsDoc());
  }

  public void testHook5() {
    AstRoot root = parse("true ? 1 : /** attach */ 2;");
    ConditionalExpression hook = (ConditionalExpression)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(hook.getFalseExpression().getJsDoc());
  }

  public void testIf1() {
    AstRoot root = parse("if (/** attach */ x) {}");
    IfStatement ifstm = (IfStatement) root.getFirstChild();
    assertNotNull(ifstm.getCondition().getJsDoc());
  }

  public void testIf2() {
    AstRoot root = parse("if (x) /** don't attach */ {}");
    IfStatement ifstm = (IfStatement) root.getFirstChild();
    assertNull(ifstm.getThenPart().getJsDoc());
  }

  public void testIf3() {
    AstRoot root = parse("if (x) {} else /** don't attach */ {}");
    IfStatement ifstm = (IfStatement) root.getFirstChild();
    assertNull(ifstm.getElsePart().getJsDoc());
  }

  public void testIf4() {
    AstRoot root = parse("if (x) {} /** don't attach */ else {}");
    IfStatement ifstm = (IfStatement) root.getFirstChild();
    assertNull(ifstm.getElsePart().getJsDoc());
  }

  public void testLabeledStm1() {
    AstRoot root = parse("/** attach */ FOO: if (x) {};");
    LabeledStatement ls = (LabeledStatement) root.getFirstChild();
    assertNotNull(ls.getJsDoc());
  }

  public void testLabeledStm2() {
    AstRoot root = parse("FOO: /** don't attach */ if (x) {};");
    LabeledStatement ls = (LabeledStatement) root.getFirstChild();
    assertNull(ls.getJsDoc());
    assertNull(ls.getStatement().getJsDoc());
  }

  public void testNew1() {
    AstRoot root = parse("/** attach */ new Foo();");
    NewExpression newexp = (NewExpression)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNotNull(newexp.getJsDoc());
  }

  public void testNew2() {
    AstRoot root = parse("new /** don't attach */ Foo();");
    NewExpression newexp = (NewExpression)
        ((ExpressionStatement) root.getFirstChild()).getExpression();
    assertNull(newexp.getJsDoc());
  }

  public void testObjLit1() {
    AstRoot root = parse("({/** attach */ 1: 2});");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
    ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
    assertNotNull(lit.getElements().get(0).getLeft().getJsDoc());
  }

  public void testObjLit2() {
    AstRoot root = parse("({1: /** attach */ 2, 3: 4});");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
    ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
    assertNotNull(lit.getElements().get(0).getRight().getJsDoc());
  }

  public void testObjLit3() {
    AstRoot root = parse("({'1': /** attach */ (foo())});");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
    ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
    assertNotNull(lit.getElements().get(0).getRight().getJsDoc());
  }

  public void testPostfix1() {
    AstRoot root = parse("/** attach */ (x)++;");
    ExpressionStatement estm = (ExpressionStatement) root.getFirstChild();
    UnaryExpression ue = (UnaryExpression) estm.getExpression();
    assertNotNull(ue.getOperand().getJsDoc());
  }

  public void testPostfix2() {
    AstRoot root = parse("/** attach */ x++;");
    ExpressionStatement estm = (ExpressionStatement) root.getFirstChild();
    UnaryExpression ue = (UnaryExpression) estm.getExpression();
    assertNotNull(ue.getJsDoc());
  }

  public void testReturn1() {
    AstRoot root = parse("function f(x) { return /** string */ x; }");
    assertNotNull(root.getComments());
    assertEquals(1, root.getComments().size());
    FunctionNode function = (FunctionNode) root.getFirstChild();
    ReturnStatement rs = (ReturnStatement) function.getBody().getFirstChild();
    assertNotNull(rs.getReturnValue().getJsDoc());
  }

  public void testReturn2() {
    AstRoot root = parse("function f(x) { /** string */ return x; }");
    assertNotNull(root.getComments());
    assertEquals(1, root.getComments().size());
    FunctionNode function = (FunctionNode) root.getFirstChild();
    ReturnStatement rs = (ReturnStatement) function.getBody().getFirstChild();
    assertNotNull(rs.getReturnValue().getJsDoc());
  }

  public void testReturn3() {
    // The first comment should be attached to the parenthesis, and the
    // second comment shouldn't be attached to any local node.
    // There used to be a bug where the second comment would get attached.
    AstRoot root = parse("function f(e) { return /** 1 */(g(1 /** 2 */)); }\n");
    assertNotNull(root.getComments());
    assertEquals(2, root.getComments().size());
    FunctionNode function = (FunctionNode) root.getFirstChild();
    AstNode body = function.getBody();
    ReturnStatement retstm = (ReturnStatement) body.getFirstChild();
    String comment = retstm.getReturnValue().getJsDoc();
    assertNotNull(comment);
    assertEquals("/** 1 */", comment);
  }

  public void testSetter() {
    AstRoot root = parse("({/** attach */ set foo() {}});");
    ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
    ParenthesizedExpression pt = (ParenthesizedExpression) st.getExpression();
    ObjectLiteral lit = (ObjectLiteral) pt.getExpression();
    assertNotNull(lit.getElements().get(0).getLeft().getJsDoc());
  }

  public void testScript1() {
    AstRoot root = parse("{ 1; /** attach */ 2; }");
    ExpressionStatement st = (ExpressionStatement)
        root.getFirstChild().getLastChild();
    assertNotNull(st.getExpression().getJsDoc());
  }

  public void testScript2() {
    AstRoot root = parse("1; /** attach */ 2;");
    ExpressionStatement st = (ExpressionStatement) root.getLastChild();
    assertNotNull(st.getExpression().getJsDoc());
  }

  public void testScript3() {
    AstRoot root = parse("1;/** attach */ function f(){}");
    assertNotNull(root.getLastChild().getJsDoc());
  }

  public void testSwitch1() {
    AstRoot root = parse("switch /** attach */ (x) {}");
    SwitchStatement sw = (SwitchStatement) root.getFirstChild();
    assertNotNull(sw.getExpression().getJsDoc());
  }

  public void testSwitch2() {
    AstRoot root = parse("switch (x) { /** don't attach */ case 1: ; }");
    SwitchStatement sw = (SwitchStatement) root.getFirstChild();
    assertNull(sw.getCases().get(0).getJsDoc());
  }

  public void testSwitch3() {
    AstRoot root = parse("switch (x) { case /** attach */ 1: ; }");
    SwitchStatement sw = (SwitchStatement) root.getFirstChild();
    assertNotNull(sw.getCases().get(0).getExpression().getJsDoc());
  }

  public void testSwitch4() {
    AstRoot root = parse("switch (x) { case 1: /** don't attach */ {}; }");
    SwitchStatement sw = (SwitchStatement) root.getFirstChild();
    assertNull(sw.getCases().get(0).getStatements().get(0).getJsDoc());
  }

  public void testSwitch5() {
    AstRoot root = parse("switch (x) { default: /** don't attach */ {}; }");
    SwitchStatement sw = (SwitchStatement) root.getFirstChild();
    assertNull(sw.getCases().get(0).getStatements().get(0).getJsDoc());
  }

  public void testSwitch6() {
    AstRoot root = parse("switch (x) { case 1: /** don't attach */ }");
  }

  public void testSwitch7() {
    AstRoot root = parse(
        "switch (x) {" +
        "  case 1: " +
        "    /** attach */ y;" +
        "    /** attach */ z;" +
        "}");
    SwitchStatement sw = (SwitchStatement) root.getFirstChild();
    SwitchCase c = sw.getCases().get(0);
    ExpressionStatement s1 = (ExpressionStatement) c.getStatements().get(0);
    ExpressionStatement s2 = (ExpressionStatement) c.getStatements().get(1);
    assertNotNull(s1.getExpression().getJsDoc());
    assertNotNull(s2.getExpression().getJsDoc());
  }

  public void testThrow() {
    AstRoot root = parse("throw /** attach */ new Foo();");
    ThrowStatement th = (ThrowStatement) root.getFirstChild();
    assertNotNull(th.getExpression().getJsDoc());
  }

  public void testTryCatch1() {
    AstRoot root = parse("try {} catch (/** attach */ e) {}");
    TryStatement tryNode = (TryStatement) root.getFirstChild();
    CatchClause catchNode = tryNode.getCatchClauses().get(0);
    assertNotNull(catchNode.getVarName().getJsDoc());
  }

  public void testTryCatch2() {
    AstRoot root = parse("try {} /** don't attach */ catch (e) {}");
    TryStatement tryNode = (TryStatement) root.getFirstChild();
    CatchClause catchNode = tryNode.getCatchClauses().get(0);
    assertNull(catchNode.getJsDoc());
    assertNull(catchNode.getVarName().getJsDoc());
  }

  public void testTryCatch3() {
    AstRoot root = parse("/** @preserveTry */ try {} catch (e) {}");
    assertNotNull(root.getFirstChild().getJsDoc());
  }

  public void testTryFinally() {
    AstRoot root = parse("try {} finally { /** attach */ e; }");
    TryStatement tryNode = (TryStatement) root.getFirstChild();
    AstNode fin = tryNode.getFinallyBlock();
    ExpressionStatement estm = (ExpressionStatement) fin.getFirstChild();
    assertNotNull(estm.getExpression().getJsDoc());
  }

  public void testUnary() {
    AstRoot root = parse("!(/** attach */ x);");
    ExpressionStatement estm = (ExpressionStatement) root.getFirstChild();
    UnaryExpression ue = (UnaryExpression) estm.getExpression();
    ParenthesizedExpression pe = (ParenthesizedExpression) ue.getOperand();
    assertNotNull(pe.getExpression().getJsDoc());
  }

  public void testVar1() {
    AstRoot root = parse("/** attach */ var a;");
    assertNotNull(root.getComments());
    assertEquals(1, root.getComments().size());
    assertNotNull(root.getFirstChild().getJsDoc());
  }

  public void testVar2() {
    AstRoot root = parse("var a = /** attach */ (x);");
    assertNotNull(root.getComments());
    assertEquals(1, root.getComments().size());
    VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
    assertNotNull(vd.getVariables().get(0).getInitializer().getJsDoc());
  }

  public void testVar3() {
    AstRoot root = parse("var a = (/** attach */ {});");
    VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
    VariableInitializer vi = vd.getVariables().get(0);
    assertNotNull(((ParenthesizedExpression)
            vi.getInitializer()).getExpression().getJsDoc());
  }

  public void testVar4() {
    AstRoot root = parse("var /** number */ a = x;");
    VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
    assertNotNull(vd.getVariables().get(0).getTarget().getJsDoc());
  }

  public void testVar5() {
    AstRoot root = parse("x = 1; /** attach */ var y = 5;");
    assertNotNull(root.getComments());
    assertEquals(1, root.getComments().size());
    assertNotNull(root.getLastChild().getJsDoc());
  }

  public void testWhile1() {
    AstRoot root = parse("while (x) /** don't attach */ {}");
    WhileLoop wh = (WhileLoop) root.getFirstChild();
    assertNull(wh.getBody().getJsDoc());
  }

  public void testWhile2() {
    AstRoot root = parse("while /** attach */ (x) {}");
    WhileLoop wh = (WhileLoop) root.getFirstChild();
    assertNotNull(wh.getCondition().getJsDoc());
  }

  public void testWhile3() {
    AstRoot root = parse("while (x /** don't attach */) {}");
    WhileLoop wh = (WhileLoop) root.getFirstChild();
    assertNull(wh.getCondition().getJsDoc());
    assertNull(wh.getBody().getJsDoc());
  }

  public void testWith1() {
    AstRoot root = parse("with (/** attach */ obj) {};");
    WithStatement w = (WithStatement) root.getFirstChild();
    assertNotNull(w.getExpression().getJsDoc());
  }

  public void testWith2() {
    AstRoot root = parse("with (obj) /** don't attach */ {};");
    WithStatement w = (WithStatement) root.getFirstChild();
    assertNull(w.getStatement().getJsDoc());
  }

  public void testWith3() {
    AstRoot root = parse("with (obj /** don't attach */) {};");
    WithStatement w = (WithStatement) root.getFirstChild();
    assertNull(w.getExpression().getJsDoc());
    assertNull(w.getStatement().getJsDoc());
  }

  public void testWith4() {
    AstRoot root = parse(
        "/** @suppress {with} */ with (context) {\n" +
        "  eval('[' + expr + ']');\n" +
        "}\n");
    assertNotNull(((WithStatement) root.getFirstChild()).getJsDoc());
  }

  public void testManyComments1() {
    AstRoot root = parse(
        "function /** number */ f(/** number */ x, /** number */ y) {\n" +
        "  return x + y;\n" +
        "}");
    assertNotNull(root.getComments());
    assertEquals(3, root.getComments().size());
    FunctionNode f = (FunctionNode) root.getFirstChild();
    assertNotNull(f.getFunctionName().getJsDoc());
    for (AstNode param : f.getParams()) {
      assertNotNull(param.getJsDoc());
    }
  }

  public void testManyComments2() {
    AstRoot root = parse("var /** number */ x = 1; var /** string */ y = 2;");
    assertNotNull(root.getComments());
    assertEquals(2, root.getComments().size());
    VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
    assertNotNull(vd.getVariables().get(0).getTarget().getJsDoc());
    vd = (VariableDeclaration) root.getLastChild();
    assertNotNull(vd.getVariables().get(0).getTarget().getJsDoc());
  }

  public void testManyCommentsOnOneNode() {
    // When many jsdocs could attach to a node, we pick the last one.
    AstRoot root = parse("var x; /** foo */ /** bar */ function f() {}");
    assertEquals("/** bar */", root.getLastChild().getJsDoc());
  }

}
