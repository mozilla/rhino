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

    public void testLinenoAssign() throws Exception {
        AstRoot root = parse("a = b");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof Assignment);
        assertEquals(Token.ASSIGN, n.getType());
        assertEquals(0, n.getLineno());
    }

    public void testLinenoCall() throws Exception {
        AstRoot root = parse("\nfoo(123);");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof FunctionCall);
        assertEquals(Token.CALL, n.getType());
        assertEquals(1, n.getLineno());
    }

    public void testLinenoGetProp() throws Exception {
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

    public void testLinenoGetElem() throws Exception {
        AstRoot root = parse("\nfoo[123]");
        ExpressionStatement st = (ExpressionStatement) root.getFirstChild();
        AstNode n = st.getExpression();

        assertTrue(n instanceof ElementGet);
        assertEquals(Token.GETELEM, n.getType());
        assertEquals(1, n.getLineno());
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
