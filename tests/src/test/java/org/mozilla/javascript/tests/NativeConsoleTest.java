/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeConsole;
import org.mozilla.javascript.NativeConsole.Level;
import org.mozilla.javascript.ScriptStackElement;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityUtilities;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;

/** Test NativeConsole */
public class NativeConsoleTest {

    private static class PrinterCall {
        public Level level;
        public Object[] args;
        public ScriptStackElement[] stack;

        public PrinterCall(Level level, Object[] args, ScriptStackElement[] stack) {
            this.level = level;
            this.args = args;
            this.stack = stack;
        }

        public PrinterCall(Level level, Object[] args) {
            this(level, args, null);
        }

        public void assertEquals(PrinterCall expectedCall) {
            Assert.assertEquals(expectedCall.level, this.level);
            if (expectedCall.args != null) {
                Assert.assertEquals(expectedCall.args.length, this.args.length);
                for (int i = 0; i < expectedCall.args.length; ++i) {
                    if (expectedCall.args[i] instanceof Pattern && this.args[i] instanceof String) {
                        Assert.assertTrue(
                                "\""
                                        + this.args[i]
                                        + "\" does not matches \""
                                        + expectedCall.args[i]
                                        + "\"",
                                ((Pattern) expectedCall.args[i])
                                        .matcher((String) this.args[i])
                                        .matches());
                    } else {
                        Assert.assertEquals(expectedCall.args[i], this.args[i]);
                    }
                }
            }
            if (expectedCall.stack != null) {
                Assert.assertEquals(expectedCall.stack.length, this.stack.length);
                for (int i = 0; i < expectedCall.stack.length; ++i) {
                    Assert.assertEquals(expectedCall.stack[i].fileName, this.stack[i].fileName);
                    Assert.assertEquals(
                            expectedCall.stack[i].functionName, this.stack[i].functionName);
                    Assert.assertEquals(expectedCall.stack[i].lineNumber, this.stack[i].lineNumber);
                }
            }
        }
    }

    private static class DummyConsolePrinter implements NativeConsole.ConsolePrinter {
        private List<PrinterCall> calls = new ArrayList<>();
        private String msg;

        @Override
        public void print(
                Context cx,
                Scriptable scope,
                Level level,
                Object[] args,
                ScriptStackElement[] stack) {
            calls.add(new PrinterCall(level, args, stack));

            msg = NativeConsole.format(cx, scope, args);
            if (stack != null) {
                for (ScriptStackElement scriptStackElement : stack) {
                    msg += "\n";
                    msg += scriptStackElement;
                }
            }
        }

        public void assertCalls(List<PrinterCall> expectedCalls) {
            assertEquals(expectedCalls.size(), calls.size());
            for (int i = 0; i < calls.size(); ++i) {
                calls.get(i).assertEquals(expectedCalls.get(i));
            }
        }

        public void assertMsf(String expectedMsg) {
            assertEquals(expectedMsg, msg);
        }
    }

    @Test
    public void formatPercentSign() {
        assertFormat(new Object[] {"%%"}, "%");
        assertFormat(new Object[] {"a%%"}, "a%");
        assertFormat(new Object[] {"%%b"}, "%b");
        assertFormat(new Object[] {"a%%b"}, "a%b");
        assertFormat(new Object[] {"a%%%%b"}, "a%%b");
        assertFormat(new Object[] {"a%%c%%b"}, "a%c%b");
    }

    @Test
    public void formatString() {
        assertFormat(new Object[] {"%s", "abc"}, "abc");
        assertFormat(new Object[] {"%s", 100}, "100");
        assertFormat(new Object[] {"%s", 100.1D}, "100.1");
        assertFormat(new Object[] {"%s", Integer.MAX_VALUE}, String.valueOf(Integer.MAX_VALUE));
        assertFormat(new Object[] {"%s", Integer.MIN_VALUE}, String.valueOf(Integer.MIN_VALUE));
        assertFormat(new Object[] {"%s", Long.MAX_VALUE}, "9223372036854776000");
        assertFormat(new Object[] {"%s", Long.MIN_VALUE}, "-9223372036854776000");

        assertFormat(new Object[] {"%s", Double.NaN}, "NaN");
        assertFormat(new Object[] {"%s", Double.POSITIVE_INFINITY}, "Infinity");
        assertFormat(new Object[] {"%s", Double.NEGATIVE_INFINITY}, "-Infinity");
        assertFormat(new Object[] {"%s", Undefined.instance}, "undefined");
        assertFormat(new Object[] {"%s", SymbolKey.ITERATOR}, "Symbol(Symbol.iterator)");

        assertFormat(new Object[] {"%s", BigInteger.valueOf(100)}, "100n");
        assertFormat(
                new Object[] {"%s", new BigInteger("1234567890123456789012345678901234567890")},
                "1234567890123456789012345678901234567890n");

        assertFormat(new Object[] {"a%s", "abc"}, "aabc");
        assertFormat(new Object[] {"%sb", "abc"}, "abcb");
        assertFormat(new Object[] {"a%sb", "abc"}, "aabcb");
        assertFormat(new Object[] {"a%s%sb", "abc", "def"}, "aabcdefb");
        assertFormat(new Object[] {"a%sc%sb", "abc", "def"}, "aabccdefb");
        assertFormat(new Object[] {"a%s%sb", "abc"}, "aabc%sb");
        assertFormat(new Object[] {"a%sb"}, "a%sb");
    }

    @Test
    public void formatInt() {
        assertFormat(new Object[] {"%d", 100}, "100");
        assertFormat(new Object[] {"%d", -100}, "-100");
        assertFormat(new Object[] {"%d", Integer.MAX_VALUE}, String.valueOf(Integer.MAX_VALUE));
        assertFormat(new Object[] {"%d", Integer.MIN_VALUE}, String.valueOf(Integer.MIN_VALUE));
        assertFormat(new Object[] {"%d", Long.MAX_VALUE}, String.valueOf(Long.MAX_VALUE));
        assertFormat(new Object[] {"%d", Long.MIN_VALUE}, String.valueOf(Long.MIN_VALUE));
        assertFormat(new Object[] {"%d", 100.1D}, "100");
        assertFormat(new Object[] {"%d", -100.7D}, "-100");

        assertFormat(new Object[] {"%d", Double.NaN}, "NaN");
        assertFormat(new Object[] {"%d", Double.POSITIVE_INFINITY}, "Infinity");
        assertFormat(new Object[] {"%d", Double.NEGATIVE_INFINITY}, "-Infinity");
        assertFormat(new Object[] {"%d", Undefined.instance}, "NaN");
        assertFormat(new Object[] {"%d", SymbolKey.ITERATOR}, "NaN");

        assertFormat(new Object[] {"%d", 9007199254740991.0D}, "9007199254740991");
        assertFormat(new Object[] {"%d", -9007199254740991.0D}, "-9007199254740991");
        assertFormat(new Object[] {"%d", 9007199254740991L}, "9007199254740991");
        assertFormat(new Object[] {"%d", -9007199254740991L}, "-9007199254740991");

        assertFormat(new Object[] {"%d", BigInteger.valueOf(100)}, "100n");
        assertFormat(new Object[] {"%d", BigInteger.valueOf(-100)}, "-100n");
        assertFormat(
                new Object[] {"%d", new BigInteger("1234567890123456789012345678901234567890")},
                "1234567890123456789012345678901234567890n");

        assertFormat(new Object[] {"a%d", 100}, "a100");
        assertFormat(new Object[] {"%db", 100}, "100b");
        assertFormat(new Object[] {"a%db", 100}, "a100b");
        assertFormat(new Object[] {"a%d%db", 100, 200}, "a100200b");
        assertFormat(new Object[] {"a%dc%db", 100, 200}, "a100c200b");
        assertFormat(new Object[] {"a%d%db", 100}, "a100%db");
        assertFormat(new Object[] {"a%db"}, "a%db");
    }

    @Test
    public void formatFloat() {
        assertFormat(new Object[] {"%f", 100}, "100");
        assertFormat(new Object[] {"%f", -100}, "-100");
        assertFormat(new Object[] {"%f", Integer.MAX_VALUE}, String.valueOf(Integer.MAX_VALUE));
        assertFormat(new Object[] {"%f", Integer.MIN_VALUE}, String.valueOf(Integer.MIN_VALUE));
        assertFormat(new Object[] {"%f", Long.MAX_VALUE}, "9223372036854776000");
        assertFormat(new Object[] {"%f", Long.MIN_VALUE}, "-9223372036854776000");
        assertFormat(new Object[] {"%f", 100.1D}, "100.1");
        assertFormat(new Object[] {"%f", -100.7D}, "-100.7");
        assertFormat(
                new Object[] {"%f", Integer.MAX_VALUE + 0.1D},
                String.valueOf(Integer.MAX_VALUE) + ".1");
        assertFormat(
                new Object[] {"%f", Integer.MIN_VALUE - 0.1D},
                String.valueOf(Integer.MIN_VALUE) + ".1");

        assertFormat(new Object[] {"%f", Double.NaN}, "NaN");
        assertFormat(new Object[] {"%f", Double.POSITIVE_INFINITY}, "Infinity");
        assertFormat(new Object[] {"%f", Double.NEGATIVE_INFINITY}, "-Infinity");
        assertFormat(new Object[] {"%f", Undefined.instance}, "NaN");
        assertFormat(new Object[] {"%f", SymbolKey.ITERATOR}, "NaN");

        assertFormat(new Object[] {"%f", 9007199254740991.0D}, "9007199254740991");
        assertFormat(new Object[] {"%f", -9007199254740991.0D}, "-9007199254740991");
        assertFormat(new Object[] {"%f", 9007199254740991L}, "9007199254740991");
        assertFormat(new Object[] {"%f", -9007199254740991L}, "-9007199254740991");

        assertFormat(new Object[] {"%f", BigInteger.valueOf(100)}, "NaN");
        assertFormat(new Object[] {"%f", BigInteger.valueOf(-100)}, "NaN");

        assertFormat(new Object[] {"a%f", 100}, "a100");
        assertFormat(new Object[] {"%fb", 100}, "100b");
        assertFormat(new Object[] {"a%fb", 100}, "a100b");
        assertFormat(new Object[] {"a%f%fb", 100, 200}, "a100200b");
        assertFormat(new Object[] {"a%fc%fb", 100, 200}, "a100c200b");
        assertFormat(new Object[] {"a%f%fb", 100}, "a100%fb");
        assertFormat(new Object[] {"a%fb"}, "a%fb");
    }

    @Test
    public void formatObject() {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();

            Scriptable emptyObject = cx.newObject(scope);
            assertFormat(new Object[] {"%o", emptyObject}, "{}");

            Scriptable emptyArray = cx.newArray(scope, 0);
            assertFormat(new Object[] {"%o", emptyArray}, "[]");

            Scriptable object1 = cx.newObject(scope);
            object1.put("int1", object1, 100);
            object1.put("float1", object1, 100.1);
            object1.put("string1", object1, "abc");
            assertFormat(
                    new Object[] {"%o", object1},
                    "{" + "\"int1\":100," + "\"float1\":100.1," + "\"string1\":\"abc\"" + "}");

            Scriptable array1 = cx.newArray(scope, 0);
            array1.put(0, array1, 100);
            array1.put(1, array1, 100.1);
            array1.put(2, array1, "abc");
            assertFormat(new Object[] {"%o", array1}, "[" + "100," + "100.1," + "\"abc\"" + "]");

            Scriptable object2 = cx.newObject(scope);
            object2.put("bigint1", object2, BigInteger.valueOf(100));
            assertFormat(new Object[] {"%o", object2}, "[object Object]");
        }
    }

    @Test
    public void formatStyling() {
        assertFormat(new Object[] {"%c", "color: orange"}, "");
        assertFormat(new Object[] {"12%c34", "color: orange"}, "1234");
        assertFormat(new Object[] {"%c", "color: orange", "color: blue"}, "color: blue");

        // %c counts
        assertFormat(new Object[] {"12%c34%s", "color: orange", "ab"}, "1234ab");

        assertFormat(new Object[] {"%c"}, "%c");
    }

    @Test
    public void formatValueOnly() {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();

            assertFormat(new Object[] {"param1", "param2"}, "param1 param2");
            assertFormat(new Object[] {1, 2, 7}, "1 2 7");

            Scriptable emptyObject = cx.newObject(scope);
            assertFormat(new Object[] {emptyObject}, "{}");

            Scriptable emptyArray = cx.newArray(scope, 0);
            assertFormat(new Object[] {emptyArray}, "[]");

            Scriptable object1 = cx.newObject(scope);
            object1.put("int1", object1, 100);
            object1.put("float1", object1, 100.1);
            object1.put("string1", object1, "abc");
            assertFormat(
                    new Object[] {object1},
                    "{" + "\"int1\":100," + "\"float1\":100.1," + "\"string1\":\"abc\"" + "}");

            Scriptable array1 = cx.newArray(scope, 0);
            array1.put(0, array1, 100);
            array1.put(1, array1, 100.1);
            array1.put(2, array1, "abc");
            assertFormat(new Object[] {array1}, "[" + "100," + "100.1," + "\"abc\"" + "]");

            Scriptable object2 = cx.newObject(scope);
            object2.put("bigint1", object2, BigInteger.valueOf(100));
            assertFormat(new Object[] {object2}, "[object Object]");
        }
    }

    @Test
    public void formatMissingPlaceholder() {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();

            assertFormat(
                    new Object[] {"string: %s;", "param1", "param2"}, "string: param1; param2");
            assertFormat(new Object[] {"int: %i;", 1, 2, 7}, "int: 1; 2 7");

            Scriptable emptyObject = cx.newObject(scope);
            assertFormat(new Object[] {"", emptyObject}, "{}");

            Scriptable emptyArray = cx.newArray(scope, 0);
            assertFormat(new Object[] {"", emptyArray}, "[]");

            Scriptable object1 = cx.newObject(scope);
            object1.put("int1", object1, 100);
            object1.put("float1", object1, 100.1);
            object1.put("string1", object1, "abc");
            assertFormat(
                    new Object[] {"", object1},
                    "{" + "\"int1\":100," + "\"float1\":100.1," + "\"string1\":\"abc\"" + "}");

            Scriptable array1 = cx.newArray(scope, 0);
            array1.put(0, array1, 100);
            array1.put(1, array1, 100.1);
            array1.put(2, array1, "abc");
            assertFormat(new Object[] {"", array1}, "[" + "100," + "100.1," + "\"abc\"" + "]");

            Scriptable object2 = cx.newObject(scope);
            object2.put("bigint1", object2, BigInteger.valueOf(100));
            assertFormat(new Object[] {"", object2}, "[object Object]");
        }
    }

    @Test
    public void print() {
        assertPrintCalls(
                "console.log('abc', 123)",
                Collections.singletonList(new PrinterCall(Level.INFO, new Object[] {"abc", 123})));
        assertPrintMsg("console.log('abc', 123)", "abc 123");

        assertPrintCalls(
                "console.trace('abc', 123)",
                Collections.singletonList(
                        new PrinterCall(
                                Level.TRACE,
                                new Object[] {"abc", 123},
                                new ScriptStackElement[] {
                                    new ScriptStackElement("source", null, 1)
                                })));
        assertPrintMsg("console.trace('abc', 123)", "abc 123\n@source:1");

        assertPrintCalls(
                "console.debug('abc', 123)",
                Collections.singletonList(new PrinterCall(Level.DEBUG, new Object[] {"abc", 123})));
        assertPrintMsg("console.debug('abc', 123)", "abc 123");

        assertPrintCalls(
                "console.info('abc', 123)",
                Collections.singletonList(new PrinterCall(Level.INFO, new Object[] {"abc", 123})));
        assertPrintMsg("console.info('abc', 123)", "abc 123");

        assertPrintCalls(
                "console.warn('abc', 123)",
                Collections.singletonList(new PrinterCall(Level.WARN, new Object[] {"abc", 123})));
        assertPrintMsg("console.warn('abc', 123)", "abc 123");

        assertPrintCalls(
                "console.error('abc', 123)",
                Collections.singletonList(new PrinterCall(Level.ERROR, new Object[] {"abc", 123})));
        assertPrintMsg("console.error('abc', 123)", "abc 123");
    }

    @Test
    public void printCallable() {
        String js = "function foo() {}\n console.log(foo)";
        assertPrintMsg(js, "\"function foo() {...}\"");

        // suppress body
        js = "function fooo() { var i = 0; }\n console.log(fooo)";
        assertPrintMsg(js, "\"function fooo() {...}\"");

        js = "console.log(/abc/i)";
        assertPrintMsg(js, "\"/abc/i\"");

        js = "function foo() {}\n" + "console.log([foo, /abc/])";
        assertPrintMsg(js, "[\"function foo() {...}\",\"/abc/\"]");
    }

    @Test
    public void trace() {
        assertPrintMsg(
                "  function foo() {\n"
                        + "    function bar() {\n"
                        + "      console.trace();\n"
                        + "    }\n"
                        + "    bar();\n"
                        + "  }\n"
                        + "  foo();\n",
                "\n" + "bar()@source:3\n" + "foo()@source:5\n" + "@source:7");

        assertPrintMsg(
                "  function foo() {\n"
                        + "    function bar() {\n"
                        + "      console.trace('the word is %s', 'foo');\n"
                        + "    }\n"
                        + "    bar();\n"
                        + "  }\n"
                        + "  foo();\n",
                "the word is foo\n" + "bar()@source:3\n" + "foo()@source:5\n" + "@source:7");
    }

    @Test
    public void testAssert() {
        assertPrintCalls("console.assert(true)", Collections.emptyList());

        assertPrintCalls(
                "console.assert(false)",
                Collections.singletonList(
                        new PrinterCall(
                                Level.ERROR, new String[] {"Assertion failed: console.assert"})));
        assertPrintMsg("console.assert(false)", "Assertion failed: console.assert");

        assertPrintCalls(
                "console.assert()",
                Collections.singletonList(
                        new PrinterCall(
                                Level.ERROR, new String[] {"Assertion failed: console.assert"})));
        assertPrintMsg("console.assert()", "Assertion failed: console.assert");

        assertPrintCalls(
                "console.assert(false, 'Fail')",
                Collections.singletonList(
                        new PrinterCall(Level.ERROR, new String[] {"Assertion failed: Fail"})));
        assertPrintMsg("console.assert(false, 'Fail')", "Assertion failed: Fail");

        assertPrintCalls(
                "console.assert(false, 'Fail', 1)",
                Collections.singletonList(
                        new PrinterCall(
                                Level.ERROR, new Object[] {"Assertion failed: Fail", 1.0})));
        assertPrintMsg("console.assert(false, 'Fail', 1)", "Assertion failed: Fail 1");

        assertPrintCalls(
                "console.assert(false, 'the word is %s', 'foo')",
                Collections.singletonList(
                        new PrinterCall(
                                Level.ERROR,
                                new String[] {"Assertion failed: the word is %s", "foo"})));
        assertPrintMsg(
                "console.assert(false, 'the word is %s', 'foo')",
                "Assertion failed: the word is foo");

        assertPrintCalls(
                "console.assert(false, 42)",
                Collections.singletonList(
                        new PrinterCall(Level.ERROR, new Object[] {"Assertion failed:", 42})));
        assertPrintMsg("console.assert(false, 42)", "Assertion failed: 42");

        assertPrintMsg("console.assert(false, {a: 7})", "Assertion failed: {\"a\":7}");
    }

    @Test
    public void count() {
        assertPrintCalls(
                "console.count();\n"
                        + "console.count('a');\n"
                        + "console.count();\n"
                        + "console.count('b');\n"
                        + "console.count('b');\n"
                        + "console.countReset('b');\n"
                        + "console.countReset('c');\n"
                        + "console.count('b');\n"
                        + "console.count();\n",
                Arrays.asList(
                        new PrinterCall(Level.INFO, new String[] {"default: 1"}),
                        new PrinterCall(Level.INFO, new String[] {"a: 1"}),
                        new PrinterCall(Level.INFO, new String[] {"default: 2"}),
                        new PrinterCall(Level.INFO, new String[] {"b: 1"}),
                        new PrinterCall(Level.INFO, new String[] {"b: 2"}),
                        new PrinterCall(Level.WARN, new String[] {"Count for 'c' does not exist."}),
                        new PrinterCall(Level.INFO, new String[] {"b: 1"}),
                        new PrinterCall(Level.INFO, new String[] {"default: 3"})));
    }

    @Test
    public void time() {
        assertPrintCalls(
                "console.time();\n"
                        + "console.time('a');\n"
                        + "console.time();\n"
                        + "console.time('b');\n"
                        + "console.timeLog('b');\n"
                        + "console.timeEnd('b');\n"
                        + "console.timeLog('b');\n"
                        + "console.time('b');\n"
                        + "console.timeLog('b', 'abc', 123);\n"
                        + "console.timeLog();\n"
                        + "console.timeEnd('c');\n",
                Arrays.asList(
                        new PrinterCall(
                                Level.WARN, new Object[] {"Timer 'default' already exists."}),
                        new PrinterCall(Level.INFO, new Object[] {Pattern.compile("b: [\\d.]+ms")}),
                        new PrinterCall(Level.INFO, new Object[] {Pattern.compile("b: [\\d.]+ms")}),
                        new PrinterCall(Level.WARN, new Object[] {"Timer 'b' does not exist."}),
                        new PrinterCall(
                                Level.INFO, new Object[] {Pattern.compile("b: [\\d.]+ms abc 123")}),
                        new PrinterCall(
                                Level.INFO, new Object[] {Pattern.compile("default: [\\d.]+ms")}),
                        new PrinterCall(Level.WARN, new Object[] {"Timer 'c' does not exist."})));
    }

    @Test
    public void printConsString() {
        String js = "var msg = '['; msg += '%s'; msg += ']'; console.log(msg, 1234)";
        assertPrintMsg(js, "[1234]");
    }

    @Test
    public void printError() {
        String lf = SecurityUtilities.getSystemProperty("line.separator");
        String js =
                "try {\n"
                        + "  JSON.parse('{\"abc');\n"
                        + "} catch (e) {\n"
                        + "  console.log(e);\n"
                        + "}";
        assertPrintMsg(js, "SyntaxError: Unterminated string literal\n\tat source:2" + lf);
    }

    @Test
    public void printErrorProperty() {
        String js =
                "try {\n"
                        + "  JSON.parse('{\"abc');\n"
                        + "} catch (e) {\n"
                        + "  var obj = { msg: 'Something is wrong', err: e };\n"
                        + "  console.log(obj);\n"
                        + "}";
        assertPrintMsg(
                js,
                "{\"msg\":\"Something is wrong\",\"err\":{\"fileName\":\"source\",\"lineNumber\":2}}");
    }

    private static void assertFormat(Object[] args, String expected) {
        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();
            assertEquals(expected, NativeConsole.format(cx, scope, args));
        }
    }

    private static void assertPrintCalls(String source, List<PrinterCall> expectedCalls) {
        DummyConsolePrinter printer = new DummyConsolePrinter();

        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();
            NativeConsole.init(scope, false, printer);
            cx.evaluateString(scope, source, "source", 1, null);
            printer.assertCalls(expectedCalls);
        }
    }

    private static void assertPrintMsg(String source, String expectedMsg) {
        DummyConsolePrinter printer = new DummyConsolePrinter();

        try (Context cx = Context.enter()) {
            Scriptable scope = cx.initStandardObjects();
            NativeConsole.init(scope, false, printer);
            cx.evaluateString(scope, source, "source", 1, null);
            printer.assertMsf(expectedMsg);
        }
    }
}
