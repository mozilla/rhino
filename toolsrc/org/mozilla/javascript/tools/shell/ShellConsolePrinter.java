/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import java.io.IOException;
import java.nio.charset.Charset;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeConsole;

/** Provide a printer use in console API */
class ShellConsolePrinter implements NativeConsole.ConsolePrinter {
    private static final long serialVersionUID = 5869832740127501857L;

    @Override
    public void print(NativeConsole.Level level, String msg) {
        try {
            ShellConsole console = Main.getGlobal().getConsole(Charset.defaultCharset());
            console.println(level + " " + msg);
        } catch (IOException e) {
            throw Context.reportRuntimeError(e.getMessage());
        }
    }
}
