package org.mozilla.javascript.tests.es2022;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ES2022 RegExp d flag (hasIndices) and ES2024 v flag (unicodeSets) */
public class RegExpHasIndicesTest {

    @Test
    public void testDFlagSupport() {
        String script = "var re = /test/d; re.hasIndices === true && re.flags === 'd'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagSupport() {
        String script = "var re = /test/v; re.unicodeSets === true && re.flags === 'v'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testDFlagWithOtherFlags() {
        String script = "var re = /test/gid; re.hasIndices === true && re.flags === 'dgi'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagWithGlobal() {
        String script = "var re = /test/gv; re.unicodeSets === true && re.flags === 'gv'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testUAndVFlagsAreMutuallyExclusive() {
        String script =
                "try {"
                        + "  eval('var re = /test/uv;');"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof SyntaxError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVAndIFlagsAreIncompatible() {
        String script =
                "try {"
                        + "  eval('var re = /test/iv;');"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof SyntaxError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testRegExpConstructorWithDFlag() {
        String script = "var re = new RegExp('test', 'd'); re.hasIndices === true";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testRegExpConstructorWithVFlag() {
        String script = "var re = new RegExp('test', 'v'); re.unicodeSets === true";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testHasIndicesPropertyIsFalseWithoutDFlag() {
        String script = "var re = /test/g; re.hasIndices === false";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testUnicodeSetsPropertyIsFalseWithoutVFlag() {
        String script = "var re = /test/g; re.unicodeSets === false";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testAllFlagsInOrder() {
        // Note: u and i flags cannot be used together in Rhino currently
        String script = "var re = /test/dgmsy; re.flags === 'dgmsy'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testDFlagToString() {
        String script = "var re = /test/d; re.toString() === '/test/d'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagToString() {
        String script = "var re = /test/v; re.toString() === '/test/v'";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testRegExpConstructorUVFlagsError() {
        String script =
                "try {"
                        + "  new RegExp('test', 'uv');"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof SyntaxError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testRegExpConstructorIVFlagsError() {
        String script =
                "try {"
                        + "  new RegExp('test', 'iv');"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof SyntaxError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }
}
