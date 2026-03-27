/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.json;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.json.JsonParser;
import org.mozilla.javascript.json.JsonParser.ParseException;

public class JsonParserTest {
    private JsonParser parser;
    private Context cx;

    @BeforeEach
    public void setUp() {
        cx = Context.enter();
        parser = new JsonParser(cx, cx.initStandardObjects());
    }

    @AfterEach
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void shouldFailToParseIllegalWhitespaceChars() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue(" \u000b 1"));
    }

    @Test
    public void shouldParseJsonNull() throws Exception {
        assertEquals(null, parser.parseValue("null"));
    }

    @Test
    public void shouldFailToParseJavaNull() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue(null));
    }

    @Test
    public void shouldParseJsonBoolean() throws Exception {
        assertEquals(true, parser.parseValue("true"));
        assertEquals(false, parser.parseValue("false"));
    }

    @Test
    public void shouldParseJsonNumbers() throws Exception {
        assertEquals(1, parser.parseValue("1"));
        assertEquals(-1, parser.parseValue("-1"));
        assertEquals(1.5, parser.parseValue("1.5"));
        assertEquals(1.5e13, parser.parseValue("1.5e13"));
        assertEquals(1.0e16, parser.parseValue("9999999999999999"));
        assertEquals(Double.POSITIVE_INFINITY, parser.parseValue("1.5e99999999"));
    }

    @Test
    public void shouldFailToParseDoubleNegativeNumbers() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("--5"));
    }

    @Test
    public void shouldFailToParseNumbersWithDecimalExponent() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("5e5.5"));
    }

    @Test
    public void shouldFailToParseNumbersBeginningWithZero() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("05"));
    }

    @Test
    public void shouldParseJsonString() throws Exception {
        assertEquals("hello", parser.parseValue("\"hello\""));
        assertEquals(
                "Sch\u00f6ne Gr\u00fc\u00dfe",
                parser.parseValue("\"Sch\\u00f6ne Gr\\u00fc\\u00dfe\""));
        assertEquals("", parser.parseValue(str('"', '"')));
        assertEquals(" ", parser.parseValue(str('"', ' ', '"')));
        assertEquals("\r", parser.parseValue(str('"', '\\', 'r', '"')));
        assertEquals("\n", parser.parseValue(str('"', '\\', 'n', '"')));
        assertEquals("\t", parser.parseValue(str('"', '\\', 't', '"')));
        assertEquals("\\", parser.parseValue(str('"', '\\', '\\', '"')));
        assertEquals("/", parser.parseValue(str('"', '/', '"')));
        assertEquals("/", parser.parseValue(str('"', '\\', '/', '"')));
        assertEquals("\"", parser.parseValue(str('"', '\\', '"', '"')));
    }

    @Test
    public void shouldFailToParseEmptyJavaString() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue(""));
    }

    @Test
    public void shouldFailToParseSingleDoubleQuote() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue(str('"')));
    }

    @Test
    public void shouldFailToParseStringContainingSingleBackslash() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue(str('"', '\\', '"')));
    }

    @Test
    public void shouldFailToParseStringIllegalStringChars() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue(str('"', '\n', '"')));
    }

    @Test
    public void shouldParseEmptyJsonArray() throws Exception {
        assertEquals(0, ((NativeArray) parser.parseValue("[]")).getLength());
    }

    @Test
    public void shouldParseHeterogeneousJsonArray() throws Exception {
        NativeArray actual = (NativeArray) parser.parseValue("[ \"hello\" , 3, null, [false] ]");
        assertEquals("hello", actual.get(0, actual));
        assertEquals(3, actual.get(1, actual));
        assertEquals(null, actual.get(2, actual));

        NativeArray innerArr = (NativeArray) actual.get(3, actual);
        assertEquals(false, innerArr.get(0, innerArr));

        assertEquals(4, actual.getLength());
    }

    @Test
    public void shouldFailToParseArrayWithInvalidElements() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("[wtf]"));
    }

    @Test
    public void shouldParseJsonObject() throws Exception {
        String json =
                "{" + "\"bool\" : false, " + "\"str\"  : \"xyz\", " + "\"obj\"  : {\"a\":1} " + "}";
        NativeObject actual = (NativeObject) parser.parseValue(json);
        assertEquals(false, actual.get("bool", actual));
        assertEquals("xyz", actual.get("str", actual));
        assertArrayEquals(
                new Object[] {"bool", "str", "obj"},
                actual.getIds(),
                "Property ordering should match");

        NativeObject innerObj = (NativeObject) actual.get("obj", actual);
        assertEquals(1, innerObj.get("a", innerObj));
    }

    @Test
    public void testECMAKeyOrdering() throws Exception {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            String json =
                    "{\"foo\": \"a\", \"bar\": \"b\", \"1\": \"c\", \"-1\": \"d\", \"x\": \"e\"}";
            NativeObject actual = (NativeObject) parser.parseValue(json);
            // Ensure that modern ECMAScript property ordering works, which depends on
            // valid index values being treated as numbers and not as strings.
            assertArrayEquals(
                    new Object[] {1, "foo", "bar", "-1", "x"},
                    actual.getIds(),
                    "Property ordering should match");
        }
    }

    @Test
    public void shouldFailToParseJsonObjectsWithInvalidFormat() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("{\"only\", \"keys\"}"));
    }

    @Test
    public void shouldFailToParseMoreThanOneToplevelValue() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("1 2"));
    }

    @Test
    public void shouldFailToParseStringTruncatedUnicode() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("\"\\u00f\""));
    }

    @Test
    public void shouldFailToParseStringControlChars1() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("\"\u0000\""));
    }

    @Test
    public void shouldFailToParseStringControlChars2() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("\"\u001f\""));
    }

    @Test
    public void shouldAllowTrailingWhitespace() throws Exception {
        parser.parseValue("1 ");
    }

    @Test
    public void shouldThrowParseExceptionWhenIncompleteObject() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("{\"a\" "));
    }

    @Test
    public void shouldThrowParseExceptionWhenIncompleteArray() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("[1 "));
    }

    @Test
    public void shouldFailToParseIllegalUnicodeEscapeSeq() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("\"\\u-123\""));
    }

    @Test
    public void shouldFailToParseIllegalUnicodeEscapeSeq2() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("\"\\u006\u0661\""));
    }

    @Test
    public void shouldFailToParseIllegalUnicodeEscapeSeq3() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("\"\\u006١\""));
    }

    @Test
    public void shouldFailToParseTrailingCommaInObject1() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("{\"a\": 1,}"));
    }

    @Test
    public void shouldFailToParseTrailingCommaInObject2() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("{,\"a\": 1}"));
    }

    @Test
    public void shouldFailToParseTrailingCommaInObject3() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("{,}"));
    }

    @Test
    public void shouldParseEmptyObject() throws Exception {
        parser.parseValue("{}");
    }

    @Test
    public void shouldFailToParseTrailingCommaInArray1() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("[1,]"));
    }

    @Test
    public void shouldFailToParseTrailingCommaInArray2() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("[,1]"));
    }

    @Test
    public void shouldFailToParseTrailingCommaInArray3() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("[,]"));
    }

    @Test
    public void shouldParseEmptyArray() throws Exception {
        parser.parseValue("[]");
    }

    @Test
    public void shouldFailToParseIllegalNumber() throws Exception {
        assertThrows(ParseException.class, () -> parser.parseValue("1."));
    }

    private String str(char... chars) {
        return new String(chars);
    }
}
