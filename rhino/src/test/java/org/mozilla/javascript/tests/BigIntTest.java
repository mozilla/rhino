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

    @Test
    public void asUintN() {
        // Test the specific issue reported in GitHub issue #1573
        Utils.assertWithAllModes_ES6("18446744073709551615", "BigInt.asUintN(64, -1n).toString()");
        Utils.assertWithAllModes_ES6("4294967295", "BigInt.asUintN(32, -1n).toString()");
        Utils.assertWithAllModes_ES6("65535", "BigInt.asUintN(16, -1n).toString()");
        Utils.assertWithAllModes_ES6("255", "BigInt.asUintN(8, -1n).toString()");
        Utils.assertWithAllModes_ES6("15", "BigInt.asUintN(4, -1n).toString()");
        Utils.assertWithAllModes_ES6("1", "BigInt.asUintN(1, -1n).toString()");
        
        // Test positive values
        Utils.assertWithAllModes_ES6("255", "BigInt.asUintN(8, 255n).toString()");
        Utils.assertWithAllModes_ES6("0", "BigInt.asUintN(8, 256n).toString()");
        Utils.assertWithAllModes_ES6("1", "BigInt.asUintN(8, 257n).toString()");
        
        // Test zero bits edge case
        Utils.assertWithAllModes_ES6("0", "BigInt.asUintN(0, 123n).toString()");
        
        // Test equality with expected BigInt values (the main issue case)
        Utils.assertWithAllModes_ES6(true, "BigInt.asUintN(64, -1n) === BigInt('0xffffffffffffffff')");
    }

    @Test
    public void asIntN() {
        // Test signed truncation
        Utils.assertWithAllModes_ES6("127", "BigInt.asIntN(8, 127n).toString()");
        Utils.assertWithAllModes_ES6("-128", "BigInt.asIntN(8, 128n).toString()");
        Utils.assertWithAllModes_ES6("-1", "BigInt.asIntN(8, 255n).toString()");
        Utils.assertWithAllModes_ES6("0", "BigInt.asIntN(8, 256n).toString()");
        
        // Test negative values
        Utils.assertWithAllModes_ES6("-1", "BigInt.asIntN(8, -1n).toString()");
        Utils.assertWithAllModes_ES6("-128", "BigInt.asIntN(8, -128n).toString()");
        Utils.assertWithAllModes_ES6("127", "BigInt.asIntN(8, -129n).toString()");
        
        // Test 32-bit boundaries
        Utils.assertWithAllModes_ES6("2147483647", "BigInt.asIntN(32, 2147483647n).toString()");
        Utils.assertWithAllModes_ES6("-2147483648", "BigInt.asIntN(32, 2147483648n).toString()");
        Utils.assertWithAllModes_ES6("-1", "BigInt.asIntN(32, 4294967295n).toString()");
        
        // Test zero bits edge case
        Utils.assertWithAllModes_ES6("0", "BigInt.asIntN(0, 123n).toString()");
    }
}
