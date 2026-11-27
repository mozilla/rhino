/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScopeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression tests for the property-read fast paths introduced by SlotMapLinker. In compiled mode,
 * PROP:GET/GETNOWARN/GETWITHTHIS/GETWITHTHISOPTIONAL call sites whose receiver is a SlotMapOwner
 * are linked to targets that query the object's own slot map and walk the prototype chain directly
 * instead of going through ScriptRuntime. These tests pin the semantics that fast path must
 * preserve: prototype walks with the correct "this", the legacy __noSuchMethod__ search,
 * strict-mode warnings, optional chaining, relinking across receiver types, and VarScope receiver
 * delegation. Every test runs in all evaluation modes so compiled and interpreted results must
 * agree.
 */
public class SlotMapLinkerTest {

    @Test
    void ownAndMissingPropertyReads() {
        String script =
                "" + "var o = {a: 1, b: 2};\n" + "[o.a, o.b, o.nope === undefined].join(\"|\");";
        Utils.assertWithAllModes("1|2|true", script);
    }

    @Test
    void prototypeChainGetterReceivesOriginalObjectAsThis() {
        String script =
                ""
                        + "var proto = {};\n"
                        + "Object.defineProperty(proto, \"g\", {\n"
                        + "    get: function() { return this.tag + \"-getter\"; },\n"
                        + "    configurable: true\n"
                        + "});\n"
                        + "var child = Object.create(proto);\n"
                        + "child.tag = \"T\";\n"
                        + "var d1 = {deep: \"d1\"};\n"
                        + "var d4 = Object.create(Object.create(Object.create(d1)));\n"
                        + "var sh = Object.create({s: \"proto\"});\n"
                        + "sh.s = \"own\";\n"
                        + "[child.g, d4.deep, sh.s].join(\"|\");";
        Utils.assertWithAllModes("T-getter|d1|own", script);
    }

    @Test
    void ownAccessorSlot() {
        String script =
                ""
                        + "var acc = {};\n"
                        + "Object.defineProperty(acc, \"x\", {get: function() { return \"acc-get\"; }});\n"
                        + "acc.x;";
        Utils.assertWithAllModes("acc-get", script);
    }

    @Test
    void methodCallsBindThisFromOwnAndPrototype() {
        String script =
                ""
                        + "var m = {m: function(a, b) { return this.base + a + b; }, base: \"B:\"};\n"
                        + "var P = {m2: function(x) { return this.base * x; }, base: 10};\n"
                        + "var C = Object.create(P);\n"
                        + "C.base = 2;\n"
                        + "[m.m(1, 2), C.m2(4)].join(\"|\");";
        Utils.assertWithAllModes("B:12|8", script);
    }

    @Test
    void nonCallableGetWithThisThrowsTypeError() {
        String script = "" + "var o = {v: 5};\n" + "try { o.notAFunc(); } catch (e) { \"\" + e; }";
        Utils.assertWithAllModes("TypeError: Cannot find function notAFunc.", script);
    }

    @Test
    void noSuchMethodSearchOnCompleteMiss() {
        String script =
                ""
                        + "var nsm = {};\n"
                        + "nsm.__noSuchMethod__ = function(prop) { return \"NSM:\" + prop; };\n"
                        + "var child = Object.create({\n"
                        + "    __noSuchMethod__: function(p) { return \"protoNSM:\" + p; }\n"
                        + "});\n"
                        + "[nsm.anything(1), child.whatever(2)].join(\"|\");";
        Utils.assertWithAllModes("NSM:anything|protoNSM:whatever", script);
    }

    @Test
    void optionalChainingCalls() {
        String script =
                ""
                        + "function g(x) { return x.q?.(); }\n"
                        + "[g(null), g({}), g({q: function() { return 42; }}), g({q: null})].join(\"|\");";
        // join() renders undefined elements as empty strings
        Utils.assertWithAllModes("||42|", script);
    }

    @Test
    void relinkAcrossReceiverTypes() {
        String script =
                ""
                        + "function f(x) { try { return x.p; } catch (e) { return \"ERR\"; } }\n"
                        + "[f({p: 1}),\n"
                        + " f(Object.defineProperty({}, \"p\", {get: function() { return 99; }})),\n"
                        + " f(Object.create({p: 7})),\n"
                        + " f({}),\n"
                        + " f([10, 20]),\n"
                        + " f(\"str\"),\n"
                        + " f(5),\n"
                        + " f(null),\n"
                        + " f({p: 2})].join(\"|\");";
        // join() renders undefined elements as empty strings
        Utils.assertWithAllModes("1|99|7|||||ERR|2", script);
    }

    @Test
    void globalThisReceiverAfterOtherTypes() {
        String script =
                ""
                        + "function h(x) { return x.zzzOwn; }\n"
                        + "h({});\n"
                        + "globalThis.zzzOwn = 9;\n"
                        + "var r = String(h(globalThis));\n"
                        + "delete globalThis.zzzOwn;\n"
                        + "r + \"|\" + h(globalThis);";
        Utils.assertWithAllModes("9|undefined", script);
    }

    @Test
    void strictModeWarnsOnceOnMissingPlainRead() {
        ContextFactory factory = Utils.contextFactoryWithFeatures(Context.FEATURE_STRICT_MODE);
        String script = "var o = {a: 1};\no.missing;";
        Utils.runWithAllModes(
                factory,
                cx -> {
                    List<String> warnings = new ArrayList<>();
                    cx.setErrorReporter(collectingReporter(warnings));
                    TopLevel scope = cx.initStandardObjects();
                    Object result = cx.evaluateString(scope, script, "test.js", 1, null);
                    assertEquals(Undefined.instance, result);
                    assertEquals(List.of("Reference to undefined property \"missing\""), warnings);
                    return null;
                });
    }

    @Test
    void strictModeNoWarnForGetNowarnReads() {
        ContextFactory factory = Utils.contextFactoryWithFeatures(Context.FEATURE_STRICT_MODE);
        String script =
                ""
                        + "var o = {a: 1};\n"
                        + "var r1 = typeof o.missing;\n"
                        + "var r2 = \"x\";\n"
                        + "if (o.missing) { r2 = \"truthy\"; }\n"
                        + "r1 + \"|\" + r2;";
        Utils.runWithAllModes(
                factory,
                cx -> {
                    List<String> warnings = new ArrayList<>();
                    cx.setErrorReporter(collectingReporter(warnings));
                    TopLevel scope = cx.initStandardObjects();
                    Object result = cx.evaluateString(scope, script, "test.js", 1, null);
                    assertEquals("undefined|x", result);
                    assertEquals(List.of(), warnings);
                    return null;
                });
    }

    @Test
    void valuesNotConfusedWithNotFound() {
        String script =
                ""
                        + "var nz = {n: null, f: false, z: 0, e: \"\"};\n"
                        + "[nz.n === null, nz.f === false, nz.z === 0, nz.e === \"\"].join(\",\");";
        Utils.assertWithAllModes("true,true,true,true", script);
    }

    @Test
    void numericStringNames() {
        String script =
                "" + "var num = {\"0\": \"zero\", 1: \"one\"};\n" + "num[\"0\"] + \"|\" + num[1];";
        Utils.assertWithAllModes("zero|one", script);
    }

    @Test
    void largeEmbeddedSlotMap() {
        StringBuilder script =
                new StringBuilder()
                        .append("var big = {};\n")
                        .append("for (var i = 0; i < 1500; i++) { big[\"k\" + i] = i; }\n")
                        .append("big.k1499 + \"|\" + (big.kMissing === undefined);\n");
        Utils.assertWithAllModes("1499|true", script.toString());
    }

    @Test
    void wrapperObjectMethods() {
        String script = "" + "\"abc\".toUpperCase() + \"|\" + (3.14159).toFixed(2);\n";
        Utils.assertWithAllModes("ABC|3.14", script);
    }

    @Test
    void varScopeReceiverUsesScopeSemantics() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    Script script =
                            cx.compileString("function f(x) { return x.p; }", "test.js", 1, null);
                    script.exec(cx, scope);
                    Function f = (Function) scope.get("f", scope);

                    // Link the call site with a plain object receiver first.
                    assertEquals(
                            "v=1",
                            cx.evaluateString(scope, "\"v=\" + f({p: 1});", "test.js", 2, null));

                    // A ScopeObject is both a SlotMapOwner and a VarScope: its own map is
                    // consulted with scope semantics, not a prototype walk.
                    VarScope inner = new ScopeObject(scope);
                    inner.put("p", inner, 7);
                    Object found = f.call(cx, scope, Undefined.instance, new Object[] {inner});
                    assertEquals(7, ((Number) found).intValue());

                    // A missing property yields the raw NOT_FOUND sentinel, matching the
                    // legacy VarScope branch of ScriptRuntime.getObjectProp.
                    VarScope empty = new ScopeObject(scope);
                    assertEquals(
                            Scriptable.NOT_FOUND,
                            f.call(cx, scope, Undefined.instance, new Object[] {empty}));
                    return null;
                });
    }

    private static ErrorReporter collectingReporter(List<String> warnings) {
        return new ErrorReporter() {
            @Override
            public void warning(
                    String message,
                    String sourceName,
                    int line,
                    String lineSource,
                    int lineOffset) {
                warnings.add(message);
            }

            @Override
            public void error(
                    String message,
                    String sourceName,
                    int line,
                    String lineSource,
                    int lineOffset) {
                fail("unexpected error: " + message);
            }

            @Override
            public EvaluatorException runtimeError(
                    String message,
                    String sourceName,
                    int line,
                    String lineSource,
                    int lineOffset) {
                return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
            }
        };
    }
}
