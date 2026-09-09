/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;

/**
 * Compiled classes keep the source of the script they came from, and a script can be longer than
 * the 64K a single string constant holds.
 */
public class ClassCompilerLargeSourceTest {

    @Test
    public void aSourceTooLongForOneStringConstantCompiles() {
        String padding = "x".repeat(300);
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            source.append("function f")
                    .append(i)
                    .append("() { return '")
                    .append(padding)
                    .append("'; }\n");
        }

        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        ClassCompiler compiler = new ClassCompiler(env);

        assertDoesNotThrow(
                () -> compiler.compileToClassFiles(source.toString(), "big.js", 1, "BigScript"));
    }
}
