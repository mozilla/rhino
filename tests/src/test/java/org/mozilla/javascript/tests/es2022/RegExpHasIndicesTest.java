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

    // ES2022 hasIndices ('d' flag) functionality tests

    @Test
    public void testIndicesPropertyPresent() {
        String script =
                "var re = /test/d;"
                        + "var match = re.exec('this is a test');"
                        + "match.indices !== undefined";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesPropertyAbsentWithoutDFlag() {
        String script =
                "var re = /test/;"
                        + "var match = re.exec('this is a test');"
                        + "match.indices === undefined";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesOverallMatch() {
        String script =
                "var re = /test/d;"
                        + "var match = re.exec('this is a test');"
                        + "match.indices[0][0] === 10 && match.indices[0][1] === 14";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesCaptureGroups() {
        String script =
                "var re = /(\\d{4})-(\\d{2})-(\\d{2})/d;"
                        + "var match = re.exec('Date: 2025-11-29');"
                        + "match.indices[1][0] === 6 && match.indices[1][1] === 10 &&"
                        + "match.indices[2][0] === 11 && match.indices[2][1] === 13 &&"
                        + "match.indices[3][0] === 14 && match.indices[3][1] === 16";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesNamedCaptureGroups() {
        String script =
                "var re = /(?<year>\\d{4})-(?<month>\\d{2})/d;"
                        + "var match = re.exec('2025-11');"
                        + "match.indices.groups.year[0] === 0 && match.indices.groups.year[1] === 4 &&"
                        + "match.indices.groups.month[0] === 5 && match.indices.groups.month[1] === 7";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesUndefinedForUnmatchedGroups() {
        String script =
                "var re = /(a)|(b)/d;"
                        + "var match = re.exec('a');"
                        + "match.indices[1] !== undefined && match.indices[2] === undefined";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesWithGlobalFlag() {
        String script =
                "var re = /(\\d+)/gd;"
                        + "var str = '1 22 333';"
                        + "var m1 = re.exec(str);"
                        + "var m2 = re.exec(str);"
                        + "var m3 = re.exec(str);"
                        + "m1.indices[0][0] === 0 && m1.indices[0][1] === 1 &&"
                        + "m2.indices[0][0] === 2 && m2.indices[0][1] === 4 &&"
                        + "m3.indices[0][0] === 5 && m3.indices[0][1] === 8";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesMatchStructure() {
        String script =
                "var re = /(test)/d;"
                        + "var match = re.exec('test');"
                        + "Array.isArray(match.indices) && "
                        + "Array.isArray(match.indices[0]) && "
                        + "Array.isArray(match.indices[1]) && "
                        + "match.indices[0].length === 2 && "
                        + "match.indices[1].length === 2";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testIndicesGroupsStructure() {
        String script =
                "var re = /(?<name>\\w+)/d;"
                        + "var match = re.exec('test');"
                        + "typeof match.indices.groups === 'object' && "
                        + "Array.isArray(match.indices.groups.name) && "
                        + "match.indices.groups.name.length === 2";
        Utils.assertWithAllModes(true, script);
    }

    // ES2024 unicodeSets ('v' flag) functionality tests

    @Test
    public void testVFlagSetSubtraction() {
        String script =
                "var re = /[a-z--[aeiou]]/v;"
                        + "re.test('b') && re.test('z') && !re.test('a') && !re.test('e')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagSetIntersection() {
        String script =
                "var re = /[a-z&&[a-m]]/v;"
                        + "re.test('a') && re.test('m') && !re.test('n') && !re.test('z')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagMultipleOperations() {
        String script =
                "var re = /[a-z--[aeiou]--[xyz]]/v;"
                        + "re.test('b') && !re.test('a') && !re.test('x')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagWithDigits() {
        String script =
                "var re = /[\\d--[5-9]]/v;" + "re.test('0') && re.test('4') && !re.test('5')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagComplexExpression() {
        String script =
                "var re = /[a-z&&[^aeiou]]/v;" // Letters that are not vowels (consonants)
                        + "re.test('b') && re.test('z') && !re.test('a') && !re.test('e')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagSetOperationWithoutVFlagFails() {
        String script =
                "try {"
                        + "  eval('var re = /[a--b]/;');"
                        + "  false;"
                        + "} catch(e) {"
                        + "  e instanceof SyntaxError;"
                        + "}";
        Utils.assertWithAllModes(true, script);
    }

    // String literal tests using \q{} syntax
    @Test
    public void testVFlagStringLiteralBasic() {
        String script =
                "var re = /[\\q{abc}]/v;" + "re.test('abc') && !re.test('ab') && !re.test('a')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagStringLiteralWithCharacter() {
        String script =
                "var re = /[\\q{foo}bar]/v;"
                        + "re.test('foo') && re.test('b') && re.test('a') && re.test('r')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagStringLiteralMultipleAlternatives() {
        String script =
                "var re = /[\\q{abc|def}]/v;"
                        + "re.test('abc') && re.test('def') && !re.test('ab')";
        Utils.assertWithAllModes(true, script);
    }
}
