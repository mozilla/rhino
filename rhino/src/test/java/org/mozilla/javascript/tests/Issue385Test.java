/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

/**
 * Expect parsing errors when encountering default values inside destructuring assignments, instead
 * of failing on some later stage (e.g. in the IRFactory).
 *
 * <p>Should be removed when support for default values is added
 *
 * <p>Keeping this around, but change the return values
 */
public class Issue385Test {
    private Context cx;
    private Scriptable scope;

    @BeforeEach
    public void setUp() {
        cx = Context.enter();
        scope = cx.initStandardObjects();
        // destructuring assignments are supported since v1.8
        cx.setLanguageVersion(Context.VERSION_1_8);
        // errors are reported in the parsing stage,
        // optimization level doesn't matter
        cx.setInterpretedMode(true);
    }

    @AfterEach
    public void tearDown() {
        Context.exit();
    }

    @org.junit.jupiter.api.Test
    public void objDestructSimple() {
        Object result = cx.evaluateString(scope, "var {a: a = 10} = {}; a", "<eval>", 1, null);
        Assertions.assertTrue(result instanceof Double);
        Assertions.assertEquals(result, 10.0);
    }

    @org.junit.jupiter.api.Test
    public void objDestructSimpleShort() {
        assertThrows(
                EvaluatorException.class,
                () ->
                        // this case can pass for the wrong reason
                        //
                        // we assume that since 'a' isn't followed by ',', ':', or '}'
                        // it must be a part of method declaration like 'var foo = {a() {}}'
                        // and so we expect to find '(' after 'a', but '=' is found instead
                        //
                        // it's here for the completeness and in case we change method parsing
                        // before destructuring default values are supported
                        cx.evaluateString(scope, "var {a = 10} = {}", "<eval>", 1, null));
    }

    @org.junit.jupiter.api.Test
    @Disabled("complex-destructuring-not-supported")
    public void objDestructComplex() {
        Object result =
                cx.evaluateString(scope, "var {a: {b} = {b: 10}} = {}; a + b", "<eval>", 1, null);
        Assertions.assertTrue(result instanceof Double);
        Assertions.assertEquals(result, 20.0);
    }

    @org.junit.jupiter.api.Test
    public void arrDestructSimple() {
        Object result = cx.evaluateString(scope, "var [a = 10] = []; a", "<eval>", 1, null);
        Assertions.assertTrue(result instanceof Double);
        Assertions.assertEquals(result, 10.0);
    }

    @org.junit.jupiter.api.Test
    @Disabled("complex-destructuring-not-supported")
    public void arrDestructComplex() {
        Object result =
                cx.evaluateString(
                        scope, "var [[a = [b] = [4]] = [2]] = []; a + b", "<eval>", 1, null);
        Assertions.assertTrue(result instanceof Double);
        Assertions.assertEquals(result, 10.0);
    }
}
