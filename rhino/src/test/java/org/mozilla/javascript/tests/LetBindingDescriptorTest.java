package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.testutils.Utils;

class LetBindingDescriptorTest {

    private static final String WITH_TEMPORARIES =
            """
            function f() {
                let a;
                const b = 1;
                var c;
                let [d] = [1];
                { function* g() {} }
            }
            f;
            """;

    private static final String LEGACY =
            """
            function f() {
                let a;
                const b = 1;
                var c;
                let [d] = [1];
            }
            f;
            """;

    private static final String REIFIED =
            """
            function f() {
                let a;
                const b = 1;
                var c;
                eval('');
            }
            f;
            """;

    @Test
    void onlyUserLetBindingsAreMarked() {
        assertLetFlags(
                Context.VERSION_ES6,
                WITH_TEMPORARIES,
                Map.of("a", true, "b", false, "c", false, "d", true, "g", false));
    }

    @Test
    void onlyUserLetBindingsAreMarkedInReifiedFunction() {
        assertLetFlags(Context.VERSION_ES6, REIFIED, Map.of("a", true, "b", false, "c", false));
    }

    @Test
    void letBindingsThatCannotBeSeenUninitializedAreNotMarkedInLocalSlots() {
        // f needs no activation, so its bindings are held in local variable slots, and only b is
        // ever accessed before its declaration
        assertLetFlags(
                Context.VERSION_ES6,
                """
                function f(c) {
                    let a = 1;
                    if (c) b;
                    let b = a;
                    return b;
                }
                f;
                """,
                Map.of("a", false, "b", true));
    }

    @Test
    void legacyLetBindingsAreNotMarked() {
        assertLetFlags(
                Context.VERSION_1_8,
                LEGACY,
                Map.of("a", false, "b", false, "c", false, "d", false));
    }

    private static void assertLetFlags(int version, String script, Map<String, Boolean> expected) {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(version);
                    var scope = cx.initStandardObjects();
                    var fn = (JSFunction) cx.evaluateString(scope, script, "test", 1, null);
                    JSDescriptor<JSFunction> desc = fn.getDescriptor();
                    Map<String, Boolean> actual = new TreeMap<>();
                    for (int i = 0; i < desc.getParamAndVarCount(); i++) {
                        String name = desc.getParamOrVarName(i);
                        if (desc.getParamOrVarLet(i)) {
                            // Neither compiler temporaries nor block generators have a TDZ.
                            assertEquals(true, expected.get(name), name);
                        }
                        if (expected.containsKey(name)) {
                            actual.put(name, desc.getParamOrVarLet(i));
                        }
                    }
                    assertEquals(new TreeMap<>(expected), actual);
                    return null;
                });
    }
}
