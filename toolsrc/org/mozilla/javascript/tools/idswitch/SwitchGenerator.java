/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tools.idswitch;

import org.mozilla.javascript.tools.ToolErrorReporter;

public class SwitchGenerator {

    private CodePrinter P;
    private ToolErrorReporter R;
    private String source_file;

    public CodePrinter getCodePrinter() { return P; }
    public void setCodePrinter(CodePrinter value) { P = value; }

    public ToolErrorReporter getReporter() { return R; }
    public void setReporter(ToolErrorReporter value) { R = value; }

    public String getSourceFileName() { return source_file; }
    public void setSourceFileName(String value) { source_file = value; }

    public void generateSwitch(IdValuePair[] pairs) {
        if (pairs.length == 0) { return; }

        int indent_level = 2;
        P.line(indent_level, "switch (s) {");

        for (int i = 0; i < pairs.length; i++) {
            P.indent(indent_level);
            P.p("case \"");
            P.p(pairs[i].id);
            P.p("\":");
            P.nl();

            P.indent(++indent_level);
            P.p("id = ");
            P.p(pairs[i].value);
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
