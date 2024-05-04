package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ast.AstRoot;

/** This is a set of tests for parsing and using BigInts. */
public class BigIntTest {

    private Context cx;
    private Scriptable global;

    @Before
    public void init() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.getWrapFactory().setJavaPrimitiveWrap(false);
        global = cx.initStandardObjects();
    }

    @After
    public void terminate() {
        Context.exit();
    }

    @Test
    public void parse() throws IOException {
        String[] INPUTS =
                new String[] {"0n", "12n", "-12n", "1234567890987654321n", "-1234567890987654321n"};
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        for (String input : INPUTS) {
            String stmt = "x = " + input + ";\n";
            AstRoot root = new Parser(env).parse(stmt, "bigint.js", 1);
            assertEquals(stmt, root.toSource());
        }
    }
}
