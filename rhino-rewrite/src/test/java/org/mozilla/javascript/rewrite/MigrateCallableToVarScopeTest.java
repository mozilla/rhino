/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.rewrite;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

/**
 * Tests for {@link MigrateCallableToVarScope}.
 *
 * <p>Each test provides "before" Java source and "after" Java source. OpenRewrite runs the recipe
 * and asserts the result matches the expected "after" state.
 *
 * <p>OpenRewrite's isolated Java 17 parser resolves Rhino types from a JAR placed under {@code
 * META-INF/rewrite/classpath/} in the test resources, loaded via {@code
 * classpathFromResources("rhino")}. The Gradle {@code copyRhinoJarToRewriteClasspath} task
 * generates this file at build time.
 */
class MigrateCallableToVarScopeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateCallableToVarScope())
                .typeValidationOptions(TypeValidation.none())
                .parser(
                        JavaParser.fromJavaVersion()
                                .classpathFromResources(new InMemoryExecutionContext(), "rhino"));
    }

    @Test
    void migratesCallableImpl() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class MyCallable implements Callable {\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, Scriptable scope, Scriptable thisObj,"
                                + " Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class MyCallable implements Callable {\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, VarScope scope, Object thisObj,"
                                + " Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void migratesConstructableImpl() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Constructable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class MyConstructable implements Constructable {\n"
                                + "    @Override\n"
                                + "    public Scriptable construct(Context cx, Scriptable scope,"
                                + " Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Constructable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class MyConstructable implements Constructable {\n"
                                + "    @Override\n"
                                + "    public Scriptable construct(Context cx, VarScope scope,"
                                + " Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void migratesIdFunctionCallImpl() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.IdFunctionCall;\n"
                                + "import org.mozilla.javascript.IdFunctionObject;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class MyIdCall implements IdFunctionCall {\n"
                                + "    @Override\n"
                                + "    public Object execIdCall(IdFunctionObject f, Context cx,"
                                + " Scriptable scope, Scriptable thisObj, Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.*;\n"
                                + "\n"
                                + "public class MyIdCall implements IdFunctionCall {\n"
                                + "    @Override\n"
                                + "    public Object execIdCall(IdFunctionObject f, Context cx,"
                                + " VarScope scope, Object thisObj, Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void doesNotModifyAlreadyMigratedCode() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class AlreadyMigrated implements Callable {\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, VarScope scope, Object thisObj,"
                                + " Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class AlreadyMigrated implements Callable {\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, VarScope scope, Object thisObj,"
                                + " Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void doesNotModifyUnrelatedMethods() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class Unrelated {\n"
                                + "    public void call(Scriptable scope) {}\n"
                                + "}"));
    }

    @Test
    void migratesCallableCallSiteScriptableCast() {
        rewriteRun(
                spec ->
                        spec.typeValidationOptions(
                                TypeValidation.builder().methodInvocations(false).build()),
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class CallSiteExample {\n"
                                + "    public Object invoke(Callable callable, Context cx, VarScope scope,"
                                + " Scriptable thisObj, Object[] args) {\n"
                                + "        return callable.call(cx, (Scriptable) scope, (Scriptable) thisObj, args);\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class CallSiteExample {\n"
                                + "    public Object invoke(Callable callable, Context cx, VarScope scope,"
                                + " Scriptable thisObj, Object[] args) {\n"
                                + "        return callable.call(cx, (VarScope) scope, (Object) thisObj, args);\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void migratesCallableLambdaExplicitlyTyped() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    Callable c = (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> null;\n"
                                + "}",
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    Callable c = (Context cx, VarScope scope, Object thisObj, Object[] args) -> null;\n"
                                + "}"));
    }

    @Test
    void migratesConstructableLambdaExplicitlyTyped() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Constructable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    Constructable c = (Context cx, Scriptable scope, Object[] args) -> null;\n"
                                + "}",
                        "import org.mozilla.javascript.Constructable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    Constructable c = (Context cx, VarScope scope, Object[] args) -> null;\n"
                                + "}"));
    }

    @Test
    void ignoresImplicitlyTypedCallableLambda() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    Callable c = (cx, scope, thisObj, args) -> null;\n"
                                + "}"));
    }

    @Test
    void migratesLambdaFunctionTarget() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.LambdaFunction;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    public void make(Scriptable parent) {\n"
                                + "        new LambdaFunction(parent, \"foo\", 1, (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> null);\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.LambdaFunction;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    public void make(Scriptable parent) {\n"
                                + "        new LambdaFunction(parent, \"foo\", 1, (Context cx, VarScope scope, Object thisObj, Object[] args) -> null);\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void migratesLambdaConstructorTarget() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.LambdaConstructor;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    public void make(Scriptable parent) {\n"
                                + "        new LambdaConstructor(parent, \"foo\", 1, (Context cx, Scriptable scope, Object[] args) -> null);\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.LambdaConstructor;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    public void make(Scriptable parent) {\n"
                                + "        new LambdaConstructor(parent, \"foo\", 1, (Context cx, VarScope scope, Object[] args) -> null);\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void migratesAnonymousClass() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    Callable c = new Callable() {\n"
                                + "        @Override\n"
                                + "        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {\n"
                                + "            return null;\n"
                                + "        }\n"
                                + "    };\n"
                                + "}",
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    Callable c = new Callable() {\n"
                                + "        @Override\n"
                                + "        public Object call(Context cx, VarScope scope, Object thisObj, Object[] args) {\n"
                                + "            return null;\n"
                                + "        }\n"
                                + "    };\n"
                                + "}"));
    }

    @Test
    void migratesScriptExec() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Script;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class MyScript implements Script {\n"
                                + "    @Override\n"
                                + "    public Object exec(Context cx, Scriptable scope, Scriptable thisObj) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Script;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class MyScript implements Script {\n"
                                + "    @Override\n"
                                + "    public Object exec(Context cx, VarScope scope, Object thisObj) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void migratesScriptExec2Param() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Script;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class MyScript implements Script {\n"
                                + "    @Override\n"
                                + "    public Object exec(Context cx, Scriptable scope) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Script;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class MyScript implements Script {\n"
                                + "    @Override\n"
                                + "    public Object exec(Context cx, VarScope scope) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void migratesInitStandardObjectsResult() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    public void init() {\n"
                                + "        Context cx = Context.enter();\n"
                                + "        Scriptable scope = cx.initStandardObjects();\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class Test {\n"
                                + "    public void init() {\n"
                                + "        Context cx = Context.enter();\n"
                                + "        VarScope scope = cx.initStandardObjects();\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void updatesJavadoc() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class MyCallable implements Callable {\n"
                                + "    /**\n"
                                + "     * @param scope the Scriptable scope\n"
                                + "     * @param thisObj the Scriptable thisObj\n"
                                + "     */\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class MyCallable implements Callable {\n"
                                + "    /**\n"
                                + "     * @param scope the VarScope scope\n"
                                + "     * @param thisObj the Object thisObj\n"
                                + "     */\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, VarScope scope, Object thisObj, Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }

    @Test
    void removesUnusedScriptableImport() {
        rewriteRun(
                java(
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.Scriptable;\n"
                                + "\n"
                                + "public class MyCallable implements Callable {\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}",
                        "import org.mozilla.javascript.Callable;\n"
                                + "import org.mozilla.javascript.Context;\n"
                                + "import org.mozilla.javascript.VarScope;\n"
                                + "\n"
                                + "public class MyCallable implements Callable {\n"
                                + "    @Override\n"
                                + "    public Object call(Context cx, VarScope scope, Object thisObj, Object[] args) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "}"));
    }
}
