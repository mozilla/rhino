package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.testutils.Utils;

/** This is a set of tests for parsing and using BigInts. */
public class BigIntTest {
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

    @Test
    public void compareWithString() {
        Utils.assertWithAllModes_ES6(true, "9007199254740992n < '9007199254740993'");
        Utils.assertWithAllModes_ES6(true, "9007199254740992n <= '9007199254740993'");
        Utils.assertWithAllModes_ES6(true, "9007199254740993n <= '9007199254740993'");
        Utils.assertWithAllModes_ES6(true, "9007199254740993n == '9007199254740993'");
        Utils.assertWithAllModes_ES6(true, "9007199254740993n >= '9007199254740993'");
        Utils.assertWithAllModes_ES6(true, "9007199254740993n >= '9007199254740992'");
        Utils.assertWithAllModes_ES6(true, "9007199254740993n > '9007199254740992'");
    }

    @Test
    public void compareStringWith() {
        Utils.assertWithAllModes_ES6(true, "'9007199254740992' < 9007199254740993n");
        Utils.assertWithAllModes_ES6(true, "'9007199254740992' <= 9007199254740993n");
        Utils.assertWithAllModes_ES6(true, "'9007199254740993' <= 9007199254740993n");
        Utils.assertWithAllModes_ES6(true, "'9007199254740993' == 9007199254740993n");
        Utils.assertWithAllModes_ES6(true, "'9007199254740993' >= 9007199254740993n");
        Utils.assertWithAllModes_ES6(true, "'9007199254740993' >= 9007199254740992n");
        Utils.assertWithAllModes_ES6(true, "'9007199254740993' > 9007199254740992n");
    }
}
