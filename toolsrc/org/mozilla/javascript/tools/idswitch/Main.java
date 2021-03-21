/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tools.idswitch;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.tools.ToolErrorReporter;

public class Main {

    private static final String SWITCH_TAG_STR = "string_id_map";
    private static final String GENERATED_TAG_STR = "generated";
    private static final String STRING_TAG_STR = "string";

    private static final int
        NORMAL_LINE        = 0,
        SWITCH_TAG         = 1,
        GENERATED_TAG      = 2,
        STRING_TAG         = 3;

    private final List<IdValuePair> all_pairs = new ArrayList<IdValuePair>();

    private ToolErrorReporter errorReporter;
    private CodePrinter codePrinter;
    private FileBody body;
    private String source_file;

    private int tag_definition_end;

    private int tag_value_start;
    private int tag_value_end;

    private static boolean isValueType(int id) {
        if (id == STRING_TAG) { return true; }
        return false;
    }

    private static String tagName(int id) {
        switch (id) {
            case SWITCH_TAG: return SWITCH_TAG_STR;
            case -SWITCH_TAG: return "/" + SWITCH_TAG_STR;
            case GENERATED_TAG: return GENERATED_TAG_STR;
            case -GENERATED_TAG: return "/" + GENERATED_TAG_STR;
        }
        return "";
    }

    void processFile(String filePath) throws IOException {
        source_file = filePath;

        body = new FileBody();

        InputStream is;
        if (filePath.equals("-")) {
            is = System.in;
        }
        else {
            is = new FileInputStream(filePath);
        }
        try {
            Reader r = new InputStreamReader(is, "ASCII");
            body.readData(r);
        }
        finally { is.close(); }

        processFile();

        if (body.wasModified()) {
            OutputStream os;
            if (filePath.equals("-")) {
                os = System.out;
            }
            else {
                os = new FileOutputStream(filePath);
            }

            try {
                Writer w = new OutputStreamWriter(os);
                body.writeData(w);
                w.flush();
            }
            finally { os.close(); }
        }
    }

    private void processFile() {
        int cur_state = 0;
        char[] buffer = body.getBuffer();

        int generated_begin = -1, generated_end = -1;
        int time_stamp_begin = -1, time_stamp_end = -1;

        body.startLineLoop();
        while (body.nextLine()) {
            int begin = body.getLineBegin();
            int end = body.getLineEnd();

            int tag_id = extractLineTagId(buffer, begin, end);
            boolean bad_tag = false;
            switch (cur_state) {
                case NORMAL_LINE:
                    if (tag_id == SWITCH_TAG) {
                        cur_state = SWITCH_TAG;
                        all_pairs.clear();
                        generated_begin = -1;
                    }
                    else if (tag_id == -SWITCH_TAG) {
                        bad_tag = true;
                    }
                    break;
                case SWITCH_TAG:
                    if (tag_id == 0) {
                        lookForIdDefinitions(buffer, begin, end, false);
                    }
                    else if (tag_id == STRING_TAG) {
                        lookForIdDefinitions(buffer, begin, end, true);
                    }
                    else if (tag_id == GENERATED_TAG) {
                        if (generated_begin >= 0) { bad_tag = true; }
                        else {
                            cur_state = GENERATED_TAG;
                            time_stamp_begin = tag_definition_end;
                            time_stamp_end = end;
                        }
                    }
                    else if (tag_id == -SWITCH_TAG) {
                        cur_state = 0;
                        if (generated_begin >= 0 && !all_pairs.isEmpty()) {
                            generateJavaCode();
                            String code = codePrinter.toString();
                            boolean different = body.setReplacement
                                (generated_begin, generated_end, code);
                            if (different) {
                                String stamp = getTimestamp();
                                body.setReplacement
                                    (time_stamp_begin, time_stamp_end, stamp);
                            }
                        }

                        break;
                    }
                    else {
                        bad_tag = true;
                    }
                    break;
                case GENERATED_TAG:
                    if (tag_id == 0) {
                        if (generated_begin < 0) { generated_begin = begin; }
                    }
                    else if (tag_id == -GENERATED_TAG) {
                        if (generated_begin < 0) { generated_begin = begin; }
                        cur_state = SWITCH_TAG;
                        generated_end = begin;
                    }
                    else {
                        bad_tag = true;
                    }
                    break;
            }
            if (bad_tag) {
                String text = ToolErrorReporter.getMessage(
                    "msg.idswitch.bad_tag_order", tagName(tag_id));
                throw errorReporter.runtimeError
                    (text, source_file, body.getLineNumber(), null, 0);
            }
        }

        if (cur_state != 0) {
            String text = ToolErrorReporter.getMessage(
                "msg.idswitch.file_end_in_switch", tagName(cur_state));
            throw errorReporter.runtimeError
                (text, source_file, body.getLineNumber(), null, 0);
        }
    }

    private static String getTimestamp() {
        SimpleDateFormat f = new SimpleDateFormat(" 'Last update:' yyyy-MM-dd HH:mm:ss z");
        return f.format(new Date());
    }

    private void generateJavaCode() {
        codePrinter.clear();

        IdValuePair[] pairs = new IdValuePair[all_pairs.size()];
        all_pairs.toArray(pairs);

        if (pairs.length == 0) { return; }

        int indent_level = 2;
        codePrinter.line(indent_level, "switch (s) {");

        for (int i = 0; i < pairs.length; i++) {
            codePrinter.indent(indent_level);
            codePrinter.p("case \"");
            codePrinter.p(pairs[i].id);
            codePrinter.p("\":");
            codePrinter.nl();

            codePrinter.indent(++indent_level);
            codePrinter.p("id = ");
            codePrinter.p(pairs[i].value);
            codePrinter.p(";");
            codePrinter.nl();

            codePrinter.line(indent_level--, "break;");
        }

        codePrinter.line(indent_level, "default:");
        codePrinter.line(++indent_level, "id = 0;");
        codePrinter.line(indent_level--, "break;");
        codePrinter.line(indent_level, "}");
    }

    private int extractLineTagId(char[] array, int cursor, int end) {
        int id = 0;
        cursor = skipWhitespace(array, cursor, end);
        int after_leading_white_space = cursor;
        cursor = lookForSlashSlash(array, cursor, end);
        if (cursor != end) {
            boolean at_line_start = (after_leading_white_space + 2 == cursor);
            cursor = skipWhitespace(array, cursor, end);
            if (cursor != end && array[cursor] == '#') {
                ++cursor;

                boolean end_tag = false;
                if (cursor != end && array[cursor] == '/') {
                    ++cursor; end_tag = true;
                }

                int tag_start = cursor;

                for (; cursor != end; ++cursor) {
                    int c = array[cursor];
                    if (c == '#' || c == '=' ||isWhitespace(c)) { break; }
                }

                if (cursor != end) {
                    int tag_end = cursor;
                    cursor = skipWhitespace(array, cursor, end);
                    if (cursor != end) {
                        int c = array[cursor];
                        if (c == '=' || c == '#') {
                            id = getTagId
                                (array, tag_start, tag_end, at_line_start);
                            if (id != 0) {
                                String bad = null;
                                if (c == '#') {
                                    if (end_tag) {
                                        id = -id;
                                        if (isValueType(id)) {
                                            bad = "msg.idswitch.no_end_usage";
                                        }
                                    }
                                    tag_definition_end = cursor + 1;
                                }
                                else  {
                                    if (end_tag) {
                                        bad = "msg.idswitch.no_end_with_value";
                                    }
                                    else if (!isValueType(id)) {
                                        bad = "msg.idswitch.no_value_allowed";
                                    }
                                    id = extractTagValue
                                        (array, cursor + 1, end, id);
                                }
                                if (bad != null) {
                                    String s = ToolErrorReporter.getMessage(
                                        bad, tagName(id));
                                    throw errorReporter.runtimeError
                                        (s, source_file, body.getLineNumber(),
                                         null, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        return id;
    }

    // Return position after first of // or end if not found
    private static int lookForSlashSlash(char[] array, int cursor, int end) {
        while (cursor + 2 <= end) {
            int c = array[cursor++];
            if (c == '/') {
                c = array[cursor++];
                if (c == '/') {
                    return cursor;
                }
            }
        }
        return end;
    }

    private int extractTagValue(char[] array, int cursor, int end, int id) {
        // cursor points after #[^#=]+=
        // ALERT: implement support for quoted strings
        boolean found = false;
        cursor = skipWhitespace(array, cursor, end);
        if (cursor != end) {
            int value_start = cursor;
            int value_end = cursor;
            while (cursor != end) {
                int c = array[cursor];
                if (isWhitespace(c)) {
                    int after_space = skipWhitespace(array, cursor + 1, end);
                    if (after_space != end && array[after_space] == '#') {
                        value_end = cursor;
                        cursor = after_space;
                        break;
                    }
                    cursor = after_space + 1;
                }
                else if (c == '#') {
                    value_end = cursor;
                    break;
                }
                else {
                    ++cursor;
                }
            }
            if (cursor != end) {
                // array[cursor] is '#' here
                found = true;
                tag_value_start = value_start;
                tag_value_end = value_end;
                tag_definition_end = cursor + 1;
            }
        }
        return (found) ? id : 0;
    }

    private static int getTagId (char[] array, int begin, int end, boolean at_line_start) {
        if (at_line_start) {
            if (equals(SWITCH_TAG_STR, array, begin, end)) {
                return SWITCH_TAG;
            }
            if (equals(GENERATED_TAG_STR, array, begin, end)) {
                return GENERATED_TAG;
            }
        }
        if (equals(STRING_TAG_STR, array, begin, end)) {
            return STRING_TAG;
        }
        return 0;
    }

    private void lookForIdDefinitions (char[] array, int begin, int end, boolean use_tag_value_as_string) {
    // Look for the pattern
    // '^[ \t]+Id_([a-zA-Z0-9_]+)[ \t]*=.*$'
    // where \1 gives field or method name
        int cursor = begin;
        // Skip tab and spaces at the beginning
        cursor = skipWhitespace(array, cursor, end);
        int id_start = cursor;
        int name_start = skipMatchedPrefix("Id_", array, cursor, end);
        if (name_start >= 0) {
            // Found Id_ prefix
            cursor = name_start;
            cursor = skipNameChar(array, cursor, end);
            int name_end = cursor;
            if (name_start != name_end) {
                cursor = skipWhitespace(array, cursor, end);
                if (cursor != end) {
                    if (array[cursor] == '=') {
                        int id_end = name_end;
                        if (use_tag_value_as_string) {
                            name_start = tag_value_start;
                            name_end = tag_value_end;
                        }
                        // Got the match
                        addId(array, id_start, id_end, name_start, name_end);
                    }
                }
            }
        }
    }

    private void addId(char[] array, int id_start, int id_end, int name_start, int name_end) {
        String name = new String(array, name_start, name_end - name_start);
        String value = new String(array, id_start, id_end - id_start);

        IdValuePair pair = new IdValuePair(name, value);

        pair.setLineNumber(body.getLineNumber());

        all_pairs.add(pair);
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t';
    }

    private static int skipWhitespace(char[] array, int begin, int end) {
        int cursor = begin;
        for (; cursor != end; ++cursor) {
            int c = array[cursor];
            if (!isWhitespace(c)) { break; }
        }
        return cursor;
    }

    private static int skipMatchedPrefix(String prefix, char[] array, int begin, int end) {
        int cursor = -1;
        int prefix_length = prefix.length();
        if (prefix_length <= end - begin) {
            cursor = begin;
            for (int i = 0; i != prefix_length; ++i, ++cursor) {
                if (prefix.charAt(i) != array[cursor]) {
                    cursor = -1; break;
                }
            }
        }
        return cursor;
    }

    private static boolean equals(String str, char[] array, int begin, int end) {
        if (str.length() == end - begin) {
            for (int i = begin, j = 0; i != end; ++i, ++j) {
                if (array[i] != str.charAt(j)) { return false; }
            }
            return true;
        }
        return false;
    }

    private static int skipNameChar(char[] array, int begin, int end) {
        int cursor = begin;
        for (; cursor != end; ++cursor) {
            int c = array[cursor];
            if (!('a' <= c && c <= 'z') && !('A' <= c && c <= 'Z')) {
                if (!('0' <= c && c <= '9')) {
                    if (c != '_') {
                        break;
                    }
                }
            }
        }
        return cursor;
    }

    public static void main(String[] args) {
        Main self = new Main();
        int status = self.exec(args);
        System.exit(status);
    }

    private int exec(String[] args) {
        errorReporter = new ToolErrorReporter(true, System.err);

        int arg_count = processOptions(args);

        if (arg_count == 0) {
            optionError(ToolErrorReporter.getMessage(
                             "msg.idswitch.no_file_argument"));
            return -1;
        }
        if (arg_count > 1) {
            optionError(ToolErrorReporter.getMessage(
                             "msg.idswitch.too_many_arguments"));
            return -1;
        }

        codePrinter = new CodePrinter();
        codePrinter.setIndentStep(4);
        codePrinter.setIndentTabSize(0);

        try {
            processFile(args[0]);
        }
        catch (IOException ex) {
            System.err.println(ToolErrorReporter.getMessage("msg.idswitch.io_error", ex.toString()));
            return -1;
        }
        catch (EvaluatorException ex) {
            return -1;
        }
        return 0;
    }

    private static int processOptions(String[] args) {
        int status = 1;

        boolean show_usage = false;
        boolean show_version = false;

        int N = args.length;
        L: for (int i = 0; i != N; ++i) {
            String arg = args[i];
            int arg_length = arg.length();
            if (arg_length >= 2) {
                if (arg.charAt(0) == '-') {
                    if (arg.charAt(1) == '-') {
                        if (arg_length == 2) {
                            args[i] = null; break;
                        }
                        if (arg.equals("--help")) {
                            show_usage = true;
                        }
                        else if (arg.equals("--version")) {
                            show_version = true;
                        }
                        else {
                            optionError(ToolErrorReporter.getMessage(
                                             "msg.idswitch.bad_option", arg));
                            status = -1; break L;
                        }
                    }
                    else {
                        for (int j = 1; j != arg_length; ++j) {
                            char c = arg.charAt(j);
                            switch (c) {
                                case 'h': show_usage = true; break;
                                default:
                                    optionError(
                                        ToolErrorReporter.getMessage(
                                            "msg.idswitch.bad_option_char",
                                            String.valueOf(c)));
                                    status = -1;
                                    break L;
                            }

                        }
                    }
                    args[i] = null;
                }
            }
        }

        if (status == 1) {
            if (show_usage) { showUsage(); status = 0; }
            if (show_version) { showVersion(); status = 0; }
        }

        if (status != 1) { System.exit(status); }

        return removeNulls(args);
    }

    private static void showUsage() {
        System.out.println(            ToolErrorReporter.getMessage("msg.idswitch.usage"));
        System.out.println();
    }

    private static void showVersion() {
        System.out.println(ToolErrorReporter.getMessage("msg.idswitch.version"));
    }

    private static void optionError(String str) {
        System.err.println(ToolErrorReporter.getMessage("msg.idswitch.bad_invocation", str));
    }

    private static int removeNulls(String[] array) {
        int N = array.length;
        int cursor = 0;
        for (; cursor != N; ++cursor) {
            if (array[cursor] == null) { break; }
        }
        int destination = cursor;
        if (cursor != N) {
            ++cursor;
            for (; cursor != N; ++cursor) {
                String elem = array[cursor];
                if (elem != null) {
                    array[destination] = elem; ++destination;
                }
            }
        }
        return destination;
    }
}