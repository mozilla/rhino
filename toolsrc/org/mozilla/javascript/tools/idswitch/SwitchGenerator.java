/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tools.idswitch;

import org.mozilla.javascript.tools.ToolErrorReporter;

public class SwitchGenerator {

    String v_switch_label = "L0";
    String v_label = "L";
    String v_s = "s";
    String v_c = "c";
    String v_guess = "X";
    String v_id = "id";
    String v_length_suffix = "_length";

    int use_if_threshold = 3;
    int char_tail_test_threshold = 2;

    private IdValuePair[] pairs;

    private CodePrinter P;
    private ToolErrorReporter R;
    private String source_file;

    public CodePrinter getCodePrinter() { return P; }
    public void setCodePrinter(CodePrinter value) { P = value; }

    public ToolErrorReporter getReporter() { return R; }
    public void setReporter(ToolErrorReporter value) { R = value; }

    public String getSourceFileName() { return source_file; }
    public void setSourceFileName(String value) { source_file = value; }

    public void generateSwitch(String[] pairs, String default_value) {
        int N = pairs.length / 2;
        IdValuePair[] id_pairs = new IdValuePair[N];
        for (int i = 0; i != N; ++i) {
            id_pairs[i] = new IdValuePair(pairs[2 * i], pairs[2 * i + 1]);
        }
        generateSwitch(id_pairs, default_value);

    }

    public void generateSwitch(IdValuePair[] pairs, String default_value) {
        int begin = 0;
        int end = pairs.length;
        if (begin == end) { return; }
        this.pairs = pairs;

        generate_body(begin, end, 2);
    }

    private void generate_body(int begin, int end, int indent_level) {
        P.line(indent_level, "switch (s) {");

        for (int i = begin; i < end; i++) {
            P.indent(indent_level);
            P.p("case \"");
            P.p(this.pairs[i].id);
            P.p("\":");
            P.nl();

            P.indent(++indent_level);
            P.p("id = ");
            P.p(this.pairs[i].value);
            P.p(";");
            P.nl();

            P.line(indent_level--, "break;");
        }

        P.line(indent_level, "default:");
        P.line(++indent_level, "id = 0;");
        P.line(indent_level--, "break;");
        P.line(indent_level, "}");
    }
}
