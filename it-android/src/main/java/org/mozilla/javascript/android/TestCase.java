package org.mozilla.javascript.android;

import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Utility class, that search for testcases in "assets/tests".
 *
 * <p>The tests are executed in a MoziallaTestSuite-manner. It includes the assert.js by default.
 *
 * @author Roland Praml
 */
public abstract class TestCase {

    protected final String name;
    protected final Scriptable global;

    private static final ContextFactory factory =
            new ContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (featureIndex == 20 /*Context.FEATURE_ENABLE_XML_SECURE_PARSING*/)
                        return false;
                    return super.hasFeature(cx, featureIndex);
                }

                @Override
                protected Context makeContext() {
                    Context cx = super.makeContext();
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    cx.setGeneratingDebug(false);
                    cx.setOptimizationLevel(-1);
                    // cx.seal(null);
                    return cx;
                }
            };

    public TestCase(String name, Scriptable global) {
        this.name = name;
        this.global = global;
    }

    public String run() {
        Context cx = factory.enterContext();
        try {
            Scriptable scope = cx.newObject(global);
            scope.setPrototype(global);
            scope.setParentScope(null);
            return ScriptRuntime.toString(runTest(cx, scope));
        } finally {
            Context.exit();
        }
    }

    protected abstract Object runTest(Context cx, Scriptable scope);

    @Override
    public String toString() {
        return name;
    }

    public static class AssetScript extends TestCase {
        protected final AssetManager assetManager;

        public AssetScript(String name, Scriptable global, AssetManager assetManager) {
            super(name, global);
            this.assetManager = assetManager;
        }

        @Override
        protected Object runTest(Context cx, Scriptable scope) {
            try (InputStream in = assetManager.open("tests/" + name);
                    Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return cx.evaluateReader(scope, rdr, name, 1, null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static List<TestCase> getTestCases(android.content.Context context) throws IOException {

        AssetManager assetManager = context.getAssets();
        // define assert object
        ScriptableObject global;
        Context cx = factory.enterContext();
        try (InputStream in = assetManager.open("assert.js");
                Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            global = cx.initStandardObjects();
            cx.evaluateReader(global, rdr, "assert.js", 1, null);
            global.sealObject();
        } finally {
            Context.exit();
        }

        String[] files = assetManager.list("tests");
        List<TestCase> tests = new ArrayList<>();
        if (files != null) {
            for (String file : files) {
                tests.add(new TestCase.AssetScript(file, global, assetManager));
            }
        }
        tests.add(new TypeInfoFactoryTestCase("TypeInfoFactory", global));
        return tests;
    }
}
