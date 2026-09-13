/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LiveConnectSupport;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.testutils.Utils;

/**
 * Verifies that without the rhino-reflect module (LiveConnect) a script cannot reach any Java
 * functionality. The only Java code that may run is code explicitly provided by the embedding, and
 * even for objects an embedding exposes to a script, no member other than the string conversion
 * (which plain JavaScript semantics require) can be reached. The matching "
 * LiveConnectSecurityTest" in the rhino-reflect module documents the attacks these tests block.
 */
public class LiveConnectDisabledTest {

    @Test
    public void liveConnectIsNotAvailable() {
        assertFalse(LiveConnectSupport.get().isAvailable());
    }

    @Test
    public void liveConnectGlobalsAreUndefined() {
        Utils.assertWithAllModes_ES6(
                "java:undefined|Packages:undefined|getClass:undefined|JavaAdapter:undefined"
                        + "|JavaImporter:undefined|importClass:undefined|importPackage:undefined",
                "['java', 'Packages', 'getClass', 'JavaAdapter', 'JavaImporter',"
                        + " 'importClass', 'importPackage']"
                        + ".map(function (name) { return name + ':' + typeof globalThis[name]; })"
                        + ".join('|')");
    }

    @Test
    public void scriptCannotWriteToSystemOut() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured));
            Utils.runWithAllModes(
                    cx -> {
                        TopLevel scope = cx.initStandardObjects();
                        Object result =
                                cx.evaluateString(
                                        scope,
                                        "(function() {"
                                                + "try {"
                                                + "  java.lang.System.out.println('pwned-by-script');"
                                                + "  return 'no error';"
                                                + "} catch (e) {"
                                                + "  return String(e);"
                                                + "}"
                                                + "})()",
                                        "defense.js",
                                        1,
                                        null);
                        assertInstanceOf(String.class, result);
                        assertTrue(
                                ((String) result).contains("ReferenceError"),
                                "unexpected result: " + result);
                        return null;
                    });
        } finally {
            System.setOut(originalOut);
        }
        String output = new String(captured.toByteArray(), StandardCharsets.UTF_8);
        assertFalse(output.contains("pwned-by-script"), "script wrote to System.out: " + output);
    }

    @Test
    public void scriptCannotExecuteArbitraryCommands(@TempDir Path tempDir) {
        Path target = tempDir.resolve("pwned-by-script");
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    scope.put("target", scope, target.toString());
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "(function() {"
                                            + "try {"
                                            + "  java.lang.Runtime.getRuntime().exec('touch ' + target);"
                                            + "  return 'no error';"
                                            + "} catch (e) {"
                                            + "  return String(e);"
                                            + "}"
                                            + "})()",
                                    "defense.js",
                                    1,
                                    null);
                    assertInstanceOf(String.class, result);
                    assertTrue(
                            ((String) result).contains("ReferenceError"),
                            "unexpected result: " + result);
                    return null;
                });
        assertFalse(Files.exists(target), "script executed an OS command");
    }

    @Test
    public void scriptCannotLoadArbitraryClasses() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "(function() {"
                                            + "try {"
                                            + "  java.lang.Class.forName('java.net.URLClassLoader');"
                                            + "  return 'no error';"
                                            + "} catch (e) {"
                                            + "  return String(e);"
                                            + "}"
                                            + "})()",
                                    "defense.js",
                                    1,
                                    null);
                    assertInstanceOf(String.class, result);
                    assertTrue(
                            ((String) result).contains("ReferenceError"),
                            "unexpected result: " + result);
                    return null;
                });
    }

    @Test
    public void scriptCannotConstructJavaClasses() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "(function() {"
                                            + "try {"
                                            + "  new java.lang.String('x');"
                                            + "  return 'no error';"
                                            + "} catch (e) {"
                                            + "  return String(e);"
                                            + "}"
                                            + "})()",
                                    "defense.js",
                                    1,
                                    null);
                    assertInstanceOf(String.class, result);
                    assertTrue(
                            ((String) result).contains("ReferenceError"),
                            "unexpected result: " + result);
                    return null;
                });
    }

    @Test
    public void propertyAccessOnExposedJavaObjectIsBlocked() {
        Utils.runWithAllModes(
                cx -> {
                    boolean[] secretCalled = {false};
                    boolean[] toStringCalled = {false};
                    TopLevel scope = cx.initStandardObjects();
                    scope.put("pojo", scope, new ExposedPojo(secretCalled, toStringCalled));
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "(function() {"
                                            + "var r = [];"
                                            + "try { pojo.secret(); r.push('secret:called'); }"
                                            + "catch (e) { r.push('secret:threw'); }"
                                            + "try { var x = pojo.bogus; r.push('bogus:got'); }"
                                            + "catch (e) { r.push('bogus:threw'); }"
                                            + "try { delete pojo.bogus; r.push('delete:ok'); }"
                                            + "catch (e) { r.push('delete:threw'); }"
                                            + "try { for (var k in pojo) { r.push('in:iterated'); } }"
                                            + "catch (e) { r.push('in:threw'); }"
                                            + "try { Object.keys(pojo); r.push('keys:ok'); }"
                                            + "catch (e) { r.push('keys:threw'); }"
                                            + "try { new Iterator(pojo); r.push('iterator:ok'); }"
                                            + "catch (e) { r.push('iterator:threw'); }"
                                            + "return r.join('|');"
                                            + "})()",
                                    "defense.js",
                                    1,
                                    null);
                    assertEquals(
                            "secret:threw|bogus:threw|delete:threw|in:threw|keys:threw|iterator:threw",
                            result);
                    assertFalse(secretCalled[0], "secret() was called");
                    assertFalse(toStringCalled[0], "toString() was called");
                    return null;
                });
    }

    @Test
    public void stringConversionOfExposedObjectIsTheOnlyJavaCall() {
        Utils.runWithAllModes(
                cx -> {
                    boolean[] secretCalled = {false};
                    boolean[] toStringCalled = {false};
                    TopLevel scope = cx.initStandardObjects();
                    scope.put("pojo", scope, new ExposedPojo(secretCalled, toStringCalled));
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "(function() {"
                                            + "return String(pojo)"
                                            + "  + '|' + Number(pojo)"
                                            + "  + '|' + (pojo instanceof Object);"
                                            + "})()",
                                    "defense.js",
                                    1,
                                    null);
                    // String conversion is required by plain JavaScript semantics and is the only
                    // Java method a script can invoke on an object the embedding exposed.
                    assertEquals("POJO-TO-STRING|NaN|false", result);
                    assertFalse(secretCalled[0], "secret() was called");
                    assertTrue(toStringCalled[0], "toString() was not called");
                    return null;
                });
    }

    @Test
    public void javaToJsIsBlockedForJavaObjects() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    ExposedPojo pojo = new ExposedPojo(new boolean[1], new boolean[1]);
                    assertThrows(EcmaError.class, () -> Context.javaToJS(pojo, scope));
                    // JS primitives still convert fine
                    assertEquals("text", Context.javaToJS("text", scope));
                    return null;
                });
    }

    @Test
    public void jsToJavaOnlyConvertsPrimitives() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    // Non-primitive target types must be rejected
                    assertThrows(EcmaError.class, () -> Context.jsToJava("x", ExposedPojo.class));
                    // Primitive conversions still work
                    assertEquals("x", Context.jsToJava("x", String.class));
                    assertEquals(Integer.valueOf(42), Context.jsToJava(42.0, Integer.class));
                    assertEquals(Boolean.TRUE, Context.jsToJava(true, Boolean.class));
                    return null;
                });
    }

    @Test
    public void caughtJavaExceptionDoesNotExposeJavaObject() {
        ContextFactory factory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        if (featureIndex == Context.FEATURE_ENHANCED_JAVA_ACCESS) {
                            return true;
                        }
                        return super.hasFeature(cx, featureIndex);
                    }
                };
        Utils.runWithAllModes(
                factory,
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    scope.put("boom", scope, new ThrowingFunction());
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "(function() {"
                                            + "try {"
                                            + "  boom();"
                                            + "  return 'no error';"
                                            + "} catch (e) {"
                                            + "  return 'name=' + e.name"
                                            + "    + '|hasJavaException=' + ('javaException' in e)"
                                            + "    + '|hasRhinoException=' + ('rhinoException' in e);"
                                            + "}"
                                            + "})()",
                                    "defense.js",
                                    1,
                                    null);
                    assertEquals(
                            "name=JavaException|hasJavaException=false|hasRhinoException=false",
                            result);
                    return null;
                });
    }

    @Test
    public void embedderFunctionCannotReturnJavaObjects() {
        Utils.runWithAllModes(
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    scope.put("retObj", scope, new ObjectReturningFunction());
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "(function() {"
                                            + "try {"
                                            + "  var r = retObj();"
                                            + "  return 'got:' + (typeof r);"
                                            + "} catch (e) {"
                                            + "  return 'blocked';"
                                            + "}"
                                            + "})()",
                                    "defense.js",
                                    1,
                                    null);
                    assertEquals("blocked", result);
                    return null;
                });
    }

    /** A Java object an embedding exposes to a script. */
    public static class ExposedPojo {
        private final boolean[] secretCalled;
        private final boolean[] toStringCalled;

        public ExposedPojo(boolean[] secretCalled, boolean[] toStringCalled) {
            this.secretCalled = secretCalled;
            this.toStringCalled = toStringCalled;
        }

        public String secret() {
            secretCalled[0] = true;
            return "TOP-SECRET";
        }

        @Override
        public String toString() {
            toStringCalled[0] = true;
            return "POJO-TO-STRING";
        }
    }

    /** A function the embedding provides that throws a Java exception. */
    public static class ThrowingFunction extends ScriptableObject implements Function {
        @Override
        public String getClassName() {
            return "Function";
        }

        @Override
        public Object call(Context cx, VarScope scope, Object thisObj, Object[] args) {
            throw new IllegalStateException("boom-from-java");
        }

        @Override
        public Scriptable construct(Context cx, Object nt, VarScope s, Object[] args) {
            throw new UnsupportedOperationException();
        }

        public int getArity() {
            return 0;
        }

        public String getFunctionName() {
            return "boom";
        }
    }

    /** A function the embedding provides that returns a plain Java object. */
    public static class ObjectReturningFunction extends ScriptableObject implements Function {
        @Override
        public String getClassName() {
            return "Function";
        }

        @Override
        public Object call(Context cx, VarScope scope, Object thisObj, Object[] args) {
            return new Object();
        }

        @Override
        public Scriptable construct(Context cx, Object nt, VarScope s, Object[] args) {
            throw new UnsupportedOperationException();
        }

        public int getArity() {
            return 0;
        }

        public String getFunctionName() {
            return "retObj";
        }
    }
}
