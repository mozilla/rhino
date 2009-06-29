package org.mozilla.javascript.tests;

import org.mozilla.javascript.ast.*;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.testing.TestErrorReporter;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

public class ParserTest extends TestCase {

    public void testLinenoAssign() {
        AstRoot root = parse("\n\na = b");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof Assignment);
        assertEquals(Token.ASSIGN, n.getType());
        assertEquals(2, n.getLineno());
    }

    public void testLinenoCall() {
        AstRoot root = parse("\nfoo(123);");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof FunctionCall);
        assertEquals(Token.CALL, n.getType());
        assertEquals(1, n.getLineno());
    }

    public void testLinenoGetProp() {
        AstRoot root = parse("\nfoo.bar");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof PropertyGet);
        assertEquals(Token.GETPROP, n.getType());
        assertEquals(1, n.getLineno());

        PropertyGet getprop = (PropertyGet) n;
        AstNode m = getprop.getRight();

        assertTrue(m instanceof Name);
        assertEquals(Token.NAME, m.getType()); // used to be Token.STRING!
        assertEquals(1, m.getLineno());
    }

    public void testLinenoGetElem() {
        AstRoot root = parse("\nfoo[123]");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof ElementGet);
        assertEquals(Token.GETELEM, n.getType());
        assertEquals(1, n.getLineno());
    }

    public void testLinenoComment() {
        AstRoot root = parse("\n/** a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals(1, root.getComments().first().getLineno());
    }

    public void testLinenoComment2() {
        AstRoot root = parse("\n/**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals(1, root.getComments().first().getLineno());
    }

    public void testLinenoComment3() {
        AstRoot root = parse("\n  \n\n/**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals(3, root.getComments().first().getLineno());
    }

    public void testLinenoComment4() {
        AstRoot root = parse("\n  \n\n  /**\n\n a */");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals(3, root.getComments().first().getLineno());
    }

    public void testLineComment5() {
        AstRoot root = parse("  /**\n* a.\n* b.\n* c.*/\n");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals(0, root.getComments().first().getLineno());
    }

    public void testLineComment6() {
        AstRoot root = parse("  \n/**\n* a.\n* b.\n* c.*/\n");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals(1, root.getComments().first().getLineno());
    }

    public void testInOperatorInForLoop1() {
        parse("var a={};function b_(p){ return p;};" +
              "for(var i=b_(\"length\" in a);i<0;) {}");
    }

    public void testInOperatorInForLoop2() {
        parse("var a={}; for (;(\"length\" in a);) {}");
    }

    public void testInOperatorInForLoop3() {
        parse("for (x in y) {}");
    }

    public void testJSDocAttachment1() {
        AstRoot root = parse("/** @type number */var a;");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */",
                     root.getComments().first().getValue());
        assertNotNull(root.getFirstChild().getJsDoc());
    }

    public void testJSDocAttachment2() {
        AstRoot root = parse("/** @type number */a.b;");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */",
                     root.getComments().first().getValue());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        assertNotNull(st.getExpression().getJsDoc());
    }

    public void testJSDocAttachment3() {
        AstRoot root = parse("var a = /** @type number */(x);");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */",
                     root.getComments().first().getValue());
        VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
        VariableInitializer vi = vd.getVariables().get(0);
        assertNotNull(vi.getInitializer().getJsDoc());
    }

    public void testJSDocAttachment4() {
        AstRoot root = parse("(function() {/** should not be attached */})()");
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        FunctionCall fc = (FunctionCall) st.getExpression();
        ParenthesizedExpression pe = (ParenthesizedExpression) fc.getTarget();
        assertNull(pe.getJsDoc());
    }

    public void testParsingWithoutJSDoc() {
        AstRoot root = parse("var a = /** @type number */(x);", false);
        assertNotNull(root.getComments());
        assertEquals(1, root.getComments().size());
        assertEquals("/** @type number */",
                     root.getComments().first().getValue());
        VariableDeclaration vd = (VariableDeclaration) root.getFirstChild();
        VariableInitializer vi = vd.getVariables().get(0);
        assertTrue(vi.getInitializer() instanceof ParenthesizedExpression);
    }

    public void testParseCommentsAsReader() throws IOException {
        AstRoot root = parseAsReader(
            "/** a */var a;\n /** b */var b; /** c */var c;");
        assertNotNull(root.getComments());
        assertEquals(3, root.getComments().size());
        Comment[] comments = new Comment[3];
        comments = root.getComments().toArray(comments);
        assertEquals("/** a */", comments[0].getValue());
        assertEquals("/** b */", comments[1].getValue());
        assertEquals("/** c */", comments[2].getValue());
    }

    public void testParseCommentsAsReader2() throws IOException {
        String js = "";
        for (int i = 0; i < 100; i++) {
            String stri = Integer.toString(i);
            js += "/** Some comment for a" + stri + " */" +
                  "var a" + stri + " = " + stri + ";\n";
        }
        AstRoot root = parseAsReader(js);
    }
    
    private AstRoot parse(String string) {
        return parse(string, true);    
    }

    private AstRoot parse(String string, boolean jsdoc) {
        CompilerEnvirons environment = new CompilerEnvirons();

        TestErrorReporter testErrorReporter = new TestErrorReporter(null, null);
        environment.setErrorReporter(testErrorReporter);

        environment.setRecordingComments(true);
        environment.setRecordingLocalJsDocComments(jsdoc);

        Parser p = new Parser(environment, testErrorReporter);
        AstRoot script = p.parse(string, null, 0);

        assertTrue(testErrorReporter.hasEncounteredAllErrors());
        assertTrue(testErrorReporter.hasEncounteredAllWarnings());

        return script;
    }

    private AstRoot parseAsReader(String string) throws IOException {
        CompilerEnvirons environment = new CompilerEnvirons();

        TestErrorReporter testErrorReporter = new TestErrorReporter(null, null);
        environment.setErrorReporter(testErrorReporter);

        environment.setRecordingComments(true);
        environment.setRecordingLocalJsDocComments(true);

        Parser p = new Parser(environment, testErrorReporter);
        AstRoot script = p.parse(new StringReader(string), null, 0);

        assertTrue(testErrorReporter.hasEncounteredAllErrors());
        assertTrue(testErrorReporter.hasEncounteredAllWarnings());

        return script;
    }
}
