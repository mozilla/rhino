/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.IdeErrorReporter;

/**
 *
 * @author André Bargull
 */
@SuppressWarnings("serial")
@RunWith(Parameterized.class)
public class Bug789277Test {
    private static final String SOURCE_NAME = "<eval>";
    private static final String MISSING_SEMI = "missing ; after statement";
    private static final int SOURCE_BUFFER_LENGTH = 512; // cf. TokenStream

    private final TestData data;

    public Bug789277Test(TestData data) {
        this.data = data;
    }

    @Parameters
    public static List<Object[]> parameters() {
        List<TestData> data = new ArrayList<TestData>();
        addSimpleTests(data);
        addWhitespaceTests(data);
        addMultilineTests(data);
        addMultilineWithWhitespaceTests(data);
        addSourceBufferExceedTests(data);
        addSourceBufferAlmostExceedTests(data);
        addSourceBufferExceedPrefixTests(data);
        addSourceBufferExceedTests2(data);
        return toArray(data);
    }

    private static List<Object[]> toArray(List<?> list) {
        List<Object[]> result = new ArrayList<Object[]>();
        for (Object o : list) {
            result.add(new Object[] { o });
        }
        return result;
    }

    /**
     * Simple tests with two variable declarations separated with different line
     * separators
     */
    private static void addSimpleTests(List<TestData> data) {
        final String line1 = "var a = 3";
        final String line2 = "var b = 4;";

        class TestList extends ArrayList<TestData> {
            void add(String source) {
                Message message = newMessage(1, line1, line1.length());
                Message messageIde = newMessage(0, line1.length());
                TestData data = new TestData(source, message, messageIde);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(line1);
        list.add(line1 + "\n");
        list.add(line1 + "\r");
        list.add(line1 + "\r\n");
        list.add(line1 + "\n\r");
        list.add(line1 + "\r\r");
        list.add(line1 + "\n\n");
        list.add(line1 + "\r\n\n");
        list.add(line1 + "\r\r\n");
        list.add(line1 + "\n" + line2);
        list.add(line1 + "\r" + line2);
        list.add(line1 + "\r\n" + line2);
        list.add(line1 + "\n\r" + line2);
        list.add(line1 + "\r\r" + line2);
        list.add(line1 + "\n\n" + line2);
        list.add(line1 + "\r\n\n" + line2);
        list.add(line1 + "\r\r\n" + line2);
        data.addAll(list);
    }

    /**
     * Whitespace characters before and after two variable declarations, testing
     * the exact column offset
     */
    private static void addWhitespaceTests(List<TestData> data) {
        final String pad = "  ";
        final String line0 = "var a = 3";
        final String line1 = pad + line0 + pad;
        final String line2 = pad + "var b = 4;" + pad;

        class TestList extends ArrayList<TestData> {
            void add(String source) {
                Message message = newMessage(1, line1, pad.length() + line0.length());
                Message messageIde = newMessage(pad.length(), line0.length());
                TestData data = new TestData(source, message, messageIde);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(line1);
        list.add(line1 + "\n");
        list.add(line1 + "\r");
        list.add(line1 + "\r\n");
        list.add(line1 + "\n\r");
        list.add(line1 + "\r\r");
        list.add(line1 + "\n\n");
        list.add(line1 + "\r\n\n");
        list.add(line1 + "\r\r\n");
        list.add(line1 + "\n" + line2);
        list.add(line1 + "\r" + line2);
        list.add(line1 + "\r\n" + line2);
        list.add(line1 + "\n\r" + line2);
        list.add(line1 + "\r\r" + line2);
        list.add(line1 + "\n\n" + line2);
        list.add(line1 + "\r\n\n" + line2);
        list.add(line1 + "\r\r\n" + line2);
        data.addAll(list);
    }

    /**
     * Two variable declarations, but the first one consists of a multi-line
     * expression
     */
    private static void addMultilineTests(List<TestData> data) {
        final String line1 = "var a = 3";
        final String line2 = "+ 3";
        final String line3 = "var b = 4;";

        class TestList extends ArrayList<TestData> {
            void add(String source1, String sep, String source2) {
                String source = source1 + sep + source2;
                Message message = newMessage(1 + linebreaks(sep), line2, line2.length());
                Message messageIde = newMessage(0, line1.length() + sep.length() + line2.length());
                // warnings are reported relative to the line start in ide-mode
                Message messageIdeIde = newMessage(line1.length() + sep.length(), line2.length());
                TestData data = new TestData(source, message, messageIde, messageIdeIde);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(line1, "\n", line2);
        list.add(line1, "\r", line2);
        list.add(line1, "\r\n", line2);
        list.add(line1, "\n\r", line2);
        list.add(line1, "\r\r", line2);
        list.add(line1, "\n\n", line2);
        list.add(line1, "\r\n\n", line2);
        list.add(line1, "\r\r\n", line2);
        list.add(line1, "\r\n\r", line2);
        list.add(line1, "\n\r\r", line2);
        list.add(line1, "\n\n\r", line2);
        list.add(line1, "\n", line2 + "\n");
        list.add(line1, "\r", line2 + "\n");
        list.add(line1, "\r\n", line2 + "\n");
        list.add(line1, "\n\r", line2 + "\n");
        list.add(line1, "\r\r", line2 + "\n");
        list.add(line1, "\n\n", line2 + "\n");
        list.add(line1, "\r\n\n", line2 + "\n");
        list.add(line1, "\r\r\n", line2 + "\n");
        list.add(line1, "\r\n\r", line2 + "\n");
        list.add(line1, "\n\r\r", line2 + "\n");
        list.add(line1, "\n\n\r", line2 + "\n");
        list.add(line1, "\n", line2 + "\n" + line3);
        list.add(line1, "\r", line2 + "\n" + line3);
        list.add(line1, "\r\n", line2 + "\n" + line3);
        list.add(line1, "\n\r", line2 + "\n" + line3);
        list.add(line1, "\r\r", line2 + "\n" + line3);
        list.add(line1, "\n\n", line2 + "\n" + line3);
        list.add(line1, "\r\n\n", line2 + "\n" + line3);
        list.add(line1, "\r\r\n", line2 + "\n" + line3);
        list.add(line1, "\r\n\r", line2 + "\n" + line3);
        list.add(line1, "\n\r\r", line2 + "\n" + line3);
        list.add(line1, "\n\n\r", line2 + "\n" + line3);
        data.addAll(list);
    }

    /**
     * Two variable declarations, but the first one consists of a multi-line
     * expression
     */
    private static void addMultilineWithWhitespaceTests(List<TestData> data) {
        final String pad = "  ";
        final String line0 = "var a = 3";
        final String line1 = pad + line0;
        final String line2 = pad + "+ 3";
        final String line3 = "var b = 4;";

        class TestList extends ArrayList<TestData> {
            void add(String source1, String sep, String source2) {
                String source = source1 + sep + source2;
                Message message = newMessage(1 + linebreaks(sep), line2, line2.length());
                Message messageIde = newMessage(pad.length(),
                        line0.length() + sep.length() + line2.length());
                // warnings are reported relative to the last line in ide-mode
                Message messageIdeIde = newMessage(line1.length() + sep.length(), line2.length());
                TestData data = new TestData(source, message, messageIde, messageIdeIde);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(line1, "\n", line2);
        list.add(line1, "\r", line2);
        list.add(line1, "\r\n", line2);
        list.add(line1, "\n\r", line2);
        list.add(line1, "\r\r", line2);
        list.add(line1, "\n\n", line2);
        list.add(line1, "\r\n\n", line2);
        list.add(line1, "\r\r\n", line2);
        list.add(line1, "\r\n\r", line2);
        list.add(line1, "\n\r\r", line2);
        list.add(line1, "\n\n\r", line2);
        list.add(line1, "\n", line2 + "\n");
        list.add(line1, "\r", line2 + "\n");
        list.add(line1, "\r\n", line2 + "\n");
        list.add(line1, "\n\r", line2 + "\n");
        list.add(line1, "\r\r", line2 + "\n");
        list.add(line1, "\n\n", line2 + "\n");
        list.add(line1, "\r\n\n", line2 + "\n");
        list.add(line1, "\r\r\n", line2 + "\n");
        list.add(line1, "\r\n\r", line2 + "\n");
        list.add(line1, "\n\r\r", line2 + "\n");
        list.add(line1, "\n\n\r", line2 + "\n");
        list.add(line1, "\n", line2 + "\n" + line3);
        list.add(line1, "\r", line2 + "\n" + line3);
        list.add(line1, "\r\n", line2 + "\n" + line3);
        list.add(line1, "\n\r", line2 + "\n" + line3);
        list.add(line1, "\r\r", line2 + "\n" + line3);
        list.add(line1, "\n\n", line2 + "\n" + line3);
        list.add(line1, "\r\n\n", line2 + "\n" + line3);
        list.add(line1, "\r\r\n", line2 + "\n" + line3);
        list.add(line1, "\r\n\r", line2 + "\n" + line3);
        list.add(line1, "\n\r\r", line2 + "\n" + line3);
        list.add(line1, "\n\n\r", line2 + "\n" + line3);
        data.addAll(list);
    }

    /**
     * Testing the maximum source buffer length.
     */
    private static void addSourceBufferExceedTests(List<TestData> data) {
        final String space = times(' ', SOURCE_BUFFER_LENGTH);
        final String line1 = "var a = 3";
        final String line2 = "var b = 4;";

        class TestList extends ArrayList<TestData> {
            void add(String source1, String sep, String source2) {
                String source = source1 + sep + source2;
                Message message = newMessage(1, line1, line1.length());
                Message messageIde = newMessage(0, line1.length());
                TestData data = new TestData(source, message, messageIde);
                // TODO: adjust reader case
                Message messageR = newMessage(1 + linebreaks(sep), space + line2, space.length()
                        + "var ".length());
                data.map.put(Type.ErrorReporter_Reader, messageR);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(line1, "\n", space + line2);
        list.add(line1, "\r", space + line2);
        list.add(line1, "\r\n", space + line2);
        list.add(line1, "\n\r", space + line2);
        list.add(line1, "\r\r", space + line2);
        list.add(line1, "\n\n", space + line2);
        list.add(line1, "\r\n\n", space + line2);
        list.add(line1, "\r\r\n", space + line2);
        data.addAll(list);
    }

    /**
     * Testing the maximum source buffer length.
     */
    private static void addSourceBufferAlmostExceedTests(List<TestData> data) {
        final String line1 = "var a = 3";
        final String line2 = "var b = 4;";
        int maxSeparatorLen = 3;
        int len = SOURCE_BUFFER_LENGTH - line1.length() - maxSeparatorLen - "var ".length();
        final String space = times(' ', len);

        class TestList extends ArrayList<TestData> {
            void add(String source) {
                Message message = newMessage(1, line1, line1.length());
                Message messageIde = newMessage(0, line1.length());
                TestData data = new TestData(source, message, messageIde);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(line1 + "\n" + space + line2);
        list.add(line1 + "\r" + space + line2);
        list.add(line1 + "\r\n" + space + line2);
        list.add(line1 + "\n\r" + space + line2);
        list.add(line1 + "\r\r" + space + line2);
        list.add(line1 + "\n\n" + space + line2);
        list.add(line1 + "\r\n\n" + space + line2);
        list.add(line1 + "\r\r\n" + space + line2);
        data.addAll(list);
    }

    /**
     * Whitespace prefix does not exceed source buffer maximum length.
     */
    private static void addSourceBufferExceedPrefixTests(List<TestData> data) {
        final String space = times(' ', SOURCE_BUFFER_LENGTH);
        final String line1 = "var a = 3";
        final String line2 = "var b = 4;";

        class TestList extends ArrayList<TestData> {
            void add(String source) {
                Message message = newMessage(1, space + line1, space.length() + line1.length());
                Message messageIde = newMessage(space.length(), line1.length());
                TestData data = new TestData(source, message, messageIde);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(space + line1 + "\n" + line2);
        list.add(space + line1 + "\r" + line2);
        list.add(space + line1 + "\r\n" + line2);
        list.add(space + line1 + "\n\r" + line2);
        list.add(space + line1 + "\r\r" + line2);
        list.add(space + line1 + "\n\n" + line2);
        list.add(space + line1 + "\r\n\n" + line2);
        list.add(space + line1 + "\r\r\n" + line2);
        data.addAll(list);
    }

    /**
     * Testing the maximum source buffer length.
     */
    private static void addSourceBufferExceedTests2(List<TestData> data) {
        final String lineT = "typeof 0;\n";
        final String line1 = "var a = 3";
        final String line2 = "var b = 4;";
        final int lines = (SOURCE_BUFFER_LENGTH - line1.length()) / lineT.length();
        final String line0 = times(lineT, lines);

        class TestList extends ArrayList<TestData> {
            void add(String source1, String sep, String source2) {
                String source = source1 + sep + source2;
                Message message = newMessage(lines + 1, line1, line1.length());
                Message messageIde = newMessage(line0.length(), line1.length());
                TestData data = new TestData(source, message, messageIde);
                // TODO: adjust reader case
                Message messageR = newMessage(lines + 1 + linebreaks(sep), line2, "var ".length());
                data.map.put(Type.ErrorReporter_Reader, messageR);
                add(data);
            }
        }

        TestList list = new TestList();
        list.add(line0 + line1, "\n", line2);
        list.add(line0 + line1, "\r", line2);
        list.add(line0 + line1, "\r\n", line2);
        list.add(line0 + line1, "\n\r", line2);
        list.add(line0 + line1, "\r\r", line2);
        list.add(line0 + line1, "\n\n", line2);
        list.add(line0 + line1, "\r\n\n", line2);
        list.add(line0 + line1, "\r\r\n", line2);
        data.addAll(list);
    }

    @Test
    public void parserWithErrorReporter_String() throws IOException {
        TestErrorReporter reporter = new TestErrorReporter();
        parseStrict(data.source, reporter, false);

        assertEquals(1, reporter.warnings.size());
        assertEquals(0, reporter.errors.size());
        assertEquals(0, reporter.runtimeErrors.size());
        assertMessageEquals(data, data.map.get(Type.ErrorReporter_String), reporter.warnings.get(0));
    }

    @Test
    public void parserWithErrorReporter_Reader() throws IOException {
        TestErrorReporter reporter = new TestErrorReporter();
        parseStrict(new StringReader(data.source), reporter, false);

        assertEquals(1, reporter.warnings.size());
        assertEquals(0, reporter.errors.size());
        assertEquals(0, reporter.runtimeErrors.size());
        assertMessageEquals(data, data.map.get(Type.ErrorReporter_Reader), reporter.warnings.get(0));
    }

    @Test
    public void parserWithIdeErrorReporter_String() throws IOException {
        TestErrorReporter reporter = new TestIdeErrorReporter();
        parseStrict(data.source, reporter, false);

        assertEquals(1, reporter.warnings.size());
        assertEquals(0, reporter.errors.size());
        assertEquals(0, reporter.runtimeErrors.size());
        assertMessageEquals(data, data.map.get(Type.IdeErrorReporter_String),
                reporter.warnings.get(0));
    }

    @Test
    public void parserWithIdeErrorReporter_Reader() throws IOException {
        TestErrorReporter reporter = new TestIdeErrorReporter();
        parseStrict(new StringReader(data.source), reporter, false);

        assertEquals(1, reporter.warnings.size());
        assertEquals(0, reporter.errors.size());
        assertEquals(0, reporter.runtimeErrors.size());
        assertMessageEquals(data, data.map.get(Type.IdeErrorReporter_Reader),
                reporter.warnings.get(0));
    }

    @Test
    public void parserWithIdeErrorReporter_String_IdeMode() throws IOException {
        TestErrorReporter reporter = new TestIdeErrorReporter();
        parseStrict(data.source, reporter, true);

        assertEquals(1, reporter.warnings.size());
        assertEquals(0, reporter.errors.size());
        assertEquals(0, reporter.runtimeErrors.size());
        assertMessageEquals(data, data.map.get(Type.IdeErrorReporter_String_IdeMode),
                reporter.warnings.get(0));
    }

    @Test
    public void parserWithIdeErrorReporter_Reader_IdeMode() throws IOException {
        TestErrorReporter reporter = new TestIdeErrorReporter();
        parseStrict(new StringReader(data.source), reporter, true);

        assertEquals(1, reporter.warnings.size());
        assertEquals(0, reporter.errors.size());
        assertEquals(0, reporter.runtimeErrors.size());
        assertMessageEquals(data, data.map.get(Type.IdeErrorReporter_Reader_IdeMode),
                reporter.warnings.get(0));
    }

    private static void assertMessageEquals(TestData t, Message expected, Message actual) {
        String source = String.format("`%s`", raw(t.source));
        assertEquals(source + " [MESSAGE]", expected.message, actual.message);
        assertEquals(source + " [SOURCE_NAME]", expected.sourceName, actual.sourceName);
        assertEquals(source + " [LINE]", expected.line, actual.line);
        assertEquals(source + " [LINE_SOURCE]", expected.lineSource, actual.lineSource);
        assertEquals(source + " [LINE_OFFSET]", expected.lineOffset, actual.lineOffset);
        assertEquals(source + " [OFFSET]", expected.offset, actual.offset);
        assertEquals(source + " [LENGTH]", expected.length, actual.length);
    }

    private static boolean isLineTerminator(char c) {
        return c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029;
    }

    private static int linebreaks(String s) {
        int d = 0;
        for (int i = 0, len = s.length(); i < len; ++i) {
            char c = s.charAt(i);
            if (isLineTerminator(c)) {
                if (c == '\r' && i + 1 < len && s.charAt(i + 1) == '\n') {
                    i += 1;
                }
                d += 1;
            }
        }
        return d;
    }

    private static String times(char c, int n) {
        char[] cbuf = new char[n];
        Arrays.fill(cbuf, c);
        return new String(cbuf);
    }

    private static String times(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        while (n-- > 0) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static String raw(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = s.length(); i < len; ++i) {
            char c = s.charAt(i);
            if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static CompilerEnvirons compilerEnv(ErrorReporter reporter, boolean ide) {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.setErrorReporter(reporter);
        compilerEnv.setStrictMode(true);
        compilerEnv.setIdeMode(ide);
        return compilerEnv;
    }

    private static AstRoot parseStrict(String source, ErrorReporter reporter, boolean ide)
            throws IOException {
        Parser parser = new Parser(compilerEnv(reporter, ide));
        return parser.parse(source, SOURCE_NAME, 1);
    }

    private static AstRoot parseStrict(Reader source, ErrorReporter reporter, boolean ide)
            throws IOException {
        Parser parser = new Parser(compilerEnv(reporter, ide));
        return parser.parse(source, SOURCE_NAME, 1);
    }

    private static enum Type {
        ErrorReporter_String, ErrorReporter_Reader, IdeErrorReporter_String,
        IdeErrorReporter_Reader, IdeErrorReporter_String_IdeMode, IdeErrorReporter_Reader_IdeMode
    }

    private static class TestData {
        private final String source;
        private final Map<Type, Message> map;

        static class DataMap<K extends Enum<K>, V> extends EnumMap<K, V> {
            public DataMap(Class<K> keyType) {
                super(keyType);
            }

            public DataMap<K, V> putAll(V v, K... ks) {
                for (K k : ks) {
                    put(k, v);
                }
                return this;
            }
        }

        TestData(String source, Message message, Message messageIde) {
            this.source = source;
            this.map = new DataMap<Type, Message>(Type.class)
                    .putAll(message, Type.ErrorReporter_String, Type.ErrorReporter_Reader)
                    .putAll(messageIde, Type.IdeErrorReporter_String, Type.IdeErrorReporter_Reader)
                    .putAll(messageIde, Type.IdeErrorReporter_String_IdeMode,
                            Type.IdeErrorReporter_Reader_IdeMode);
        }

        TestData(String source, Message message, Message messageIde, Message messageIdeIde) {
            this.source = source;
            this.map = new DataMap<Type, Message>(Type.class)
                    .putAll(message, Type.ErrorReporter_String, Type.ErrorReporter_Reader)
                    .putAll(messageIde, Type.IdeErrorReporter_String, Type.IdeErrorReporter_Reader)
                    .putAll(messageIdeIde, Type.IdeErrorReporter_String_IdeMode,
                            Type.IdeErrorReporter_Reader_IdeMode);
        }
    }

    private static Message newMessage(int line, String lineSource, int lineOffset) {
        return new Message(MISSING_SEMI, SOURCE_NAME, line, lineSource, lineOffset);
    }

    private static Message newMessage(int offset, int length) {
        return new Message(MISSING_SEMI, SOURCE_NAME, offset, length);
    }

    private static class Message {
        private final String message;
        private final String sourceName;
        private final int line;
        private final String lineSource;
        private final int lineOffset;
        private final int offset;
        private final int length;

        private Message(String message, String sourceName, int line, String lineSource,
                int lineOffset) {
            this.message = message;
            this.sourceName = sourceName;
            this.line = line;
            this.lineSource = lineSource;
            this.lineOffset = lineOffset;
            this.offset = -1;
            this.length = -1;
        }

        private Message(String message, String sourceName, int offset, int length) {
            this.message = message;
            this.sourceName = sourceName;
            this.line = -1;
            this.lineSource = null;
            this.lineOffset = -1;
            this.offset = offset;
            this.length = length;
        }
    }

    private static class TestErrorReporter implements ErrorReporter {
        protected List<Message> warnings = new ArrayList<Message>();
        protected List<Message> errors = new ArrayList<Message>();
        protected List<Message> runtimeErrors = new ArrayList<Message>();

        public void warning(String message, String sourceName, int line, String lineSource,
                int lineOffset) {
            warnings.add(new Message(message, sourceName, line, lineSource, lineOffset));
        }

        public void error(String message, String sourceName, int line, String lineSource,
                int lineOffset) {
            errors.add(new Message(message, sourceName, line, lineSource, lineOffset));
        }

        public EvaluatorException runtimeError(String message, String sourceName, int line,
                String lineSource, int lineOffset) {
            runtimeErrors.add(new Message(message, sourceName, line, lineSource, lineOffset));
            return null;
        }
    }

    private static class TestIdeErrorReporter extends TestErrorReporter implements IdeErrorReporter {
        public void warning(String message, String sourceName, int offset, int length) {
            warnings.add(new Message(message, sourceName, offset, length));
        }

        public void error(String message, String sourceName, int offset, int length) {
            errors.add(new Message(message, sourceName, offset, length));
        }
    }
}
