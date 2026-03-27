/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.tools.shell.Global;

/**
 * This testcase performs various <code>instanceof</code> tests or prototype modifications in
 * different modes.
 *
 * <ul>
 *   <li>{@link Mode#GLOBAL}<br>
 *       All scripts are executed against a {@link Global} object, which holds all standard
 *       prototypes. Declarations like <code>var x=1</code> or <code>y=2</code> are stored in the
 *       global context. So different script executions will see both locally and globally changed
 *       variables.
 *   <li>{@link Mode#NESTED}<br>
 *       There is one global context and an other created with <code>
 *       scope = context.newObject(global)</code>. Prototypes are held in global context. Local
 *       variables like <code>var x=1</code> are stored in the scoped context, while global
 *       variables like <code>y=2</code> are stored in the global one. Modification on prototypes
 *       are done on the one in the global scope. Different script executions will only see globally
 *       changed variables.
 *   <li>{@link Mode#SEALED}<br>
 *       Some applications will share one sealed global context over multiple threads. On each
 *       script execution an empty scope that has the global scope as prototype is create. This will
 *       lower the memory footprint, as the global objects have to be created only one. (See
 *       https://github.com/mozilla/rhino/pull/826 for details) The drawback of this mode is, that
 *       no prototype modification can be done, because the global scope is sealed. So different
 *       script executions will not see any modifications
 *   <li>{@link Mode#SEALED_OWN_OBJECTS} in this case, no empty scope is created but every scope has
 *       its own standard-objects, which may increase the memory footprint, but some global objects
 *       can be shared.
 * </ul>
 *
 * @author Roland Praml, FOCONIS AG
 */
public class NestedContextPrototypeTest {

    protected final Global global = new Global();
    private Mode mode;

    enum Mode {
        GLOBAL,
        NESTED,
        SEALED,
        SEALED_OWN_OBJECTS;
    }

    public static Collection<Mode> data() {
        return Arrays.asList(Mode.values());
    }

    public void initNestedContextPrototypeTest(Mode mode) {
        this.mode = mode;
        boolean sealed = mode == Mode.SEALED || mode == Mode.SEALED_OWN_OBJECTS;
        global.setSealedStdLib(sealed);
        global.init(ContextFactory.getGlobal());

        ContextFactory.getGlobal()
                .call(
                        context -> {
                            context.evaluateString(
                                    global,
                                    "MyClass = function() {};\n" + "myInstance = new MyClass();",
                                    "",
                                    1,
                                    null);
                            if (sealed) {
                                global.sealObject();
                            }
                            return null;
                        });
    }

    private Object runScript(String scriptSourceText) {
        return ContextFactory.getGlobal()
                .call(
                        context -> {
                            Scriptable scope;
                            switch (mode) {
                                case GLOBAL:
                                    scope = global;
                                    break;
                                case NESTED:
                                    scope = context.newObject(global);
                                    break;
                                case SEALED:
                                    scope = TopLevel.createIsolate(global);
                                    break;
                                case SEALED_OWN_OBJECTS:
                                    scope = context.initStandardObjects(new TopLevel());
                                    ((TopLevel) scope)
                                            .getGlobalThis()
                                            .setPrototype(((TopLevel) global).getGlobalThis());
                                    break;
                                default:
                                    throw new UnsupportedOperationException();
                            }

                            return context.evaluateString(scope, scriptSourceText, "", 1, null);
                        });
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void arrayInstanceOfObject(Mode mode) {
        initNestedContextPrototypeTest(mode);
        assertEquals(true, runScript("[] instanceof Object"));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void arrayInstanceOfArray(Mode mode) {
        initNestedContextPrototypeTest(mode);
        assertEquals(true, runScript("[] instanceof Array"));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void objectInstanceOfObject(Mode mode) {
        initNestedContextPrototypeTest(mode);
        assertEquals(true, runScript("({} instanceof Object)"));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void globalInstanceInstance(Mode mode) {
        initNestedContextPrototypeTest(mode);
        assertEquals(true, runScript("myInstance instanceof MyClass"));

        if (mode == Mode.SEALED_OWN_OBJECTS) {
            // this looks a bit strange, but in this mode, the scope has its own
            // instance of Object
            assertEquals(false, runScript("myInstance instanceof this.Object"));
            assertEquals(true, runScript("myInstance instanceof this.__proto__.Object"));
            assertEquals(true, runScript("[] instanceof this.Object"));
            assertEquals(false, runScript("[] instanceof this.__proto__.Object"));
        } else {
            assertEquals(true, runScript("myInstance instanceof Object"));
        }
    }

    private static String defineFoo =
            "Object.prototype.foo = function() {\n" + " return 'bar';\n" + "};\n";

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void prototypeModification1Sealed(Mode mode) {
        assumeTrue(mode == Mode.SEALED, "check for exception in sealed mode");
        initNestedContextPrototypeTest(mode);
        assertThrows(
                EvaluatorException.class,
                () -> {
                    runScript(defineFoo + "var v=[]; (v.foo())");
                });
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void prototypeModification1(Mode mode) {
        initNestedContextPrototypeTest(mode);
        assumeFalse(mode == Mode.SEALED, "cannot define foo in sealed mode");
        assertEquals("bar", runScript(defineFoo + "var v=[]; (v.foo())"));
        if (mode == Mode.GLOBAL || mode == Mode.NESTED)
            assertEquals("bar", runScript("var v=[]; v.foo()"));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void prototypeModification2(Mode mode) {
        initNestedContextPrototypeTest(mode);
        assumeFalse(mode == Mode.SEALED, "ignored in sealed mode");
        assertEquals("bar", runScript(defineFoo + "var v={}; (v.foo())"));
        if (mode == Mode.GLOBAL || mode == Mode.NESTED)
            assertEquals("bar", runScript("var v={}; v.foo()"));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void prototypeModification3(Mode mode) {
        initNestedContextPrototypeTest(mode);
        assumeFalse(mode == Mode.SEALED, "ignored in sealed mode");
        assertEquals("bar", runScript(defineFoo + "var v=new Object(); v.foo()"));
        if (mode == Mode.GLOBAL || mode == Mode.NESTED)
            assertEquals("bar", runScript("var v=new Object(); v.foo()"));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void modeLocalVar(Mode mode) {
        initNestedContextPrototypeTest(mode);
        runScript("var v='hi'");
        if (mode == Mode.GLOBAL) {
            // only in GLOBAL mode, the local value will visible
            assertEquals("string", runScript("typeof v;"));
        } else {
            // all other modes will not see the value in different script executions
            assertEquals("undefined", runScript("typeof v;"));
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void modeGlobalVar(Mode mode) {
        initNestedContextPrototypeTest(mode);
        runScript("v='hi'");
        if (mode == Mode.GLOBAL || mode == Mode.NESTED) {
            // globally defined values will be visible in GLOBAL and NESTED mode
            assertEquals("string", runScript("typeof v;"));
        } else {
            assertEquals("undefined", runScript("typeof v;"));
        }
    }
}
