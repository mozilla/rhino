/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import java.io.IOException;
import java.nio.charset.Charset;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeConsole;
import org.mozilla.javascript.ScriptStackElement;
import org.mozilla.javascript.Scriptable;

/** Provide a printer use in console API */
class ShellConsolePrinter implements NativeConsole.ConsolePrinter {
    private static final long serialVersionUID = 5869832740127501857L;

    @Override
    public void print(
            Context cx,
            Scriptable scope,
            NativeConsole.Level level,
            Object[] args,
            ScriptStackElement[] stack) {
        if (args.length == 0) {
            return;
        }

        String msg = NativeConsole.format(cx, scope, args);
        ShellConsole console = Main.getGlobal().getConsole(Charset.defaultCharset());
        try {
            console.println(level + " " + msg);

            if (stack != null) {
                for (ScriptStackElement element : stack) {
                    console.println(element.toString());
                }
            }
        } catch (IOException e) {
            throw Context.reportRuntimeError(e.getMessage());
        }
    }
}
