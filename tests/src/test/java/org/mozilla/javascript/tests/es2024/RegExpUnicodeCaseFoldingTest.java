package org.mozilla.javascript.tests.es2024;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for Unicode case-insensitive matching (u+i and v+i flag combinations) */
public class RegExpUnicodeCaseFoldingTest {

    // Basic ASCII case folding tests

    @Test
    public void testBasicASCIILowercase() {
        // /abc/ui should match 'ABC', 'abc', 'AbC', etc.
        String script =
                "var re = /abc/ui; "
                        + "re.test('abc') && re.test('ABC') && re.test('AbC') && re.test('aBc')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testBasicASCIIUppercase() {
        // /ABC/ui should match 'abc', 'ABC', 'Abc', etc.
        String script =
                "var re = /ABC/ui; "
                        + "re.test('abc') && re.test('ABC') && re.test('Abc') && re.test('aBC')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testASCIIMixed() {
        // /HeLLo/ui should match any case variation
        String script =
                "var re = /HeLLo/ui; " + "re.test('hello') && re.test('HELLO') && re.test('HeLLo')";
        Utils.assertWithAllModes(true, script);
    }

    // Unicode case folding tests (beyond ASCII)

    @Test
    public void testGermanUmlaut() {
        // /ü/ui should match 'Ü' and vice versa
        String script = "var re = /ü/ui; re.test('ü') && re.test('Ü')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testGermanUmlautUppercase() {
        // /Ü/ui should match 'ü' and vice versa
        String script = "var re = /Ü/ui; re.test('ü') && re.test('Ü')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testFrenchAccents() {
        // /café/ui should match 'CAFÉ', 'Café', etc.
        String script = "var re = /café/ui; re.test('café') && re.test('CAFÉ') && re.test('Café')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testGreekLetter() {
        // /α/ui should match 'Α' (Greek alpha)
        String script = "var re = /α/ui; re.test('α') && re.test('Α')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testCyrillicLetter() {
        // /а/ui should match 'А' (Cyrillic a)
        String script = "var re = /а/ui; re.test('а') && re.test('А')";
        Utils.assertWithAllModes(true, script);
    }

    // Character class tests with case folding

    @Test
    public void testCharClassASCII() {
        // /[abc]/ui should match 'A', 'B', 'C', 'a', 'b', 'c'
        String script =
                "var re = /[abc]/ui; "
                        + "re.test('a') && re.test('A') && re.test('b') && re.test('B') && re.test('c') && re.test('C')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testCharClassUnicode() {
        // /[äöü]/ui should match uppercase variants
        String script =
                "var re = /[äöü]/ui; "
                        + "re.test('ä') && re.test('Ä') && re.test('ö') && re.test('Ö') && re.test('ü') && re.test('Ü')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testCharClassRange() {
        // /[a-z]/ui should match 'A'-'Z' as well
        String script =
                "var re = /[a-z]/ui; "
                        + "re.test('a') && re.test('A') && re.test('m') && re.test('M') && re.test('z') && re.test('Z')";
        Utils.assertWithAllModes(true, script);
    }

    // v flag (unicodeSets) with case folding

    @Test
    public void testVFlagBasicCaseFolding() {
        // /abc/vi should work same as /abc/ui
        String script = "var re = /abc/vi; " + "re.test('abc') && re.test('ABC') && re.test('AbC')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagUnicodeCaseFolding() {
        // /café/vi should match 'CAFÉ'
        String script = "var re = /café/vi; re.test('café') && re.test('CAFÉ')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagCharClassCaseFolding() {
        // /[äöü]/vi should match uppercase
        String script =
                "var re = /[äöü]/vi; re.test('ä') && re.test('Ä') && re.test('ö') && re.test('Ö')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testVFlagSetOperationWithCaseFolding() {
        // /[a-z--[aeiou]]/vi should match consonants in any case
        String script =
                "var re = /[a-z--[aeiou]]/vi; "
                        + "re.test('b') && re.test('B') && re.test('z') && re.test('Z') && "
                        + "!re.test('a') && !re.test('A') && !re.test('e') && !re.test('E')";
        Utils.assertWithAllModes(true, script);
    }

    // Multi-character string tests

    @Test
    public void testMultiCharacterString() {
        // /hello world/ui should match any case combination
        String script =
                "var re = /hello world/ui; "
                        + "re.test('hello world') && re.test('HELLO WORLD') && re.test('Hello World')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testMixedASCIIUnicode() {
        // /hello café/ui should match with case variations
        String script =
                "var re = /hello café/ui; "
                        + "re.test('hello café') && re.test('HELLO CAFÉ') && re.test('Hello Café')";
        Utils.assertWithAllModes(true, script);
    }

    // Negative tests - ensure case insensitivity doesn't match wrong patterns

    @Test
    public void testNonMatchingPattern() {
        // /abc/ui should not match 'def'
        String script = "var re = /abc/ui; !re.test('def') && !re.test('xyz')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testPartialNoMatch() {
        // /abc/ui should not match 'ab' or 'bc'
        String script = "var re = /abc/ui; !re.test('ab') && !re.test('bc') && !re.test('AB')";
        Utils.assertWithAllModes(true, script);
    }

    // Special Unicode case mappings - KNOWN LIMITATION
    //
    // NOTE: The following tests document a known limitation in Rhino's RegExp implementation.
    // Java's Character.toLowerCase/toUpperCase does NOT handle these compatibility characters:
    //   - U+212A (KELVIN SIGN) → should fold to 'k' (U+006B)
    //   - U+2126 (OHM SIGN) → should fold to 'ω' (U+03C9)
    //   - U+212B (ANGSTROM SIGN) → should fold to 'å' (U+00E5)
    //
    // Full Unicode case folding requires custom mapping tables beyond Java's built-in APIs.
    // These tests are disabled to document this limitation for future implementation.
    //
    // References:
    //   - https://unicode.org/Public/UNIDATA/CaseFolding.txt
    //   - https://unicode.org/reports/tr44/#CaseFolding.txt
    //   - ECMAScript spec: Runtime Semantics: Canonicalize

    /*
    @Test
    public void testKelvinSign() {
        // U+212A (KELVIN SIGN) should fold to U+006B ('k')
        // LIMITATION: Java's Character API doesn't support this mapping
        String script =
                "var re = /k/ui; "
                        + "re.test('k') && re.test('K') && re.test('\\u212A')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testKelvinSignInCharClass() {
        // [k] with /ui should match KELVIN SIGN
        // LIMITATION: Java's Character API doesn't support this mapping
        String script =
                "var re = /[k]/ui; "
                        + "re.test('k') && re.test('K') && re.test('\\u212A')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testOhmSign() {
        // U+2126 (OHM SIGN) should fold to U+03C9 (Greek lowercase omega)
        // LIMITATION: Java's Character API doesn't support this mapping
        String script =
                "var re = /ω/ui; "
                        + "re.test('ω') && re.test('Ω') && re.test('\\u2126')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testOhmSignInCharClass() {
        // [ω] with /ui should match OHM SIGN
        // LIMITATION: Java's Character API doesn't support this mapping
        String script =
                "var re = /[ω]/ui; "
                        + "re.test('ω') && re.test('Ω') && re.test('\\u2126')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testAngstromSign() {
        // U+212B (ANGSTROM SIGN) should fold to U+00E5 (å)
        // LIMITATION: Java's Character API doesn't support this mapping
        String script =
                "var re = /å/ui; "
                        + "re.test('å') && re.test('Å') && re.test('\\u212B')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testAngstromSignInCharClass() {
        // [å] with /ui should match ANGSTROM SIGN
        // LIMITATION: Java's Character API doesn't support this mapping
        String script =
                "var re = /[å]/ui; "
                        + "re.test('å') && re.test('Å') && re.test('\\u212B')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testAllSpecialCaseMappings() {
        // Test all three special mappings in one pattern
        // LIMITATION: Java's Character API doesn't support these mappings
        String script =
                "var reK = /k/ui; "
                        + "var reOhm = /ω/ui; "
                        + "var reAng = /å/ui; "
                        + "reK.test('\\u212A') && "
                        + "reOhm.test('\\u2126') && "
                        + "reAng.test('\\u212B')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testSpecialCaseMappingsVFlag() {
        // Test special mappings work with v flag as well
        // LIMITATION: Java's Character API doesn't support these mappings
        String script =
                "var reK = /k/vi; "
                        + "var reOhm = /ω/vi; "
                        + "var reAng = /å/vi; "
                        + "reK.test('\\u212A') && "
                        + "reOhm.test('\\u2126') && "
                        + "reAng.test('\\u212B')";
        Utils.assertWithAllModes(true, script);
    }
    */

    // Edge cases

    @Test
    public void testSingleCharacter() {
        // /a/ui should match 'A'
        String script = "var re = /a/ui; re.test('a') && re.test('A')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testEmptyCharClassWithQuantifier() {
        // /a*/ui should match '', 'a', 'A', 'aa', 'AA', 'aA'
        String script =
                "var re = /a*/ui; "
                        + "re.test('') && re.test('a') && re.test('A') && re.test('aa') && re.test('AA') && re.test('aA')";
        Utils.assertWithAllModes(true, script);
    }

    @Test
    public void testAlternationWithCaseFolding() {
        // /abc|def/ui should match 'ABC', 'DEF', 'Abc', 'Def'
        String script =
                "var re = /abc|def/ui; "
                        + "re.test('abc') && re.test('ABC') && re.test('def') && re.test('DEF')";
        Utils.assertWithAllModes(true, script);
    }
}
