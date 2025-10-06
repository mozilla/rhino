package org.mozilla.javascript.android;

import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.ModuleScript;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.RequireBuilder;

/**
 * Utility class, that search for testcases in "assets/tests".
 *
 * <p>The tests are executed in a MoziallaTestSuite-manner. It includes the assert.js by default.
 *
 * @author Roland Praml
 */
public class TestCase {

    private final String name;
    private final Scriptable global;
    private final AssetManager assetManager;

    private static final ContextFactory factory =
            new ContextFactory() {
                @Override
                protected boolean hasFeature(Context cx, int featureIndex) {
                    if (featureIndex == 20 /*Context.FEATURE_ENABLE_XML_SECURE_PARSING*/) return false;
                    return super.hasFeature(cx, featureIndex);
                }

                @Override
                protected Context makeContext() {
                    Context cx = super.makeContext();
                    cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                    cx.setGeneratingDebug(false);
                    cx.setOptimizationLevel(-1);
                    //cx.seal(null);
                    return cx;
                }
            };

    public TestCase(String name, Scriptable global, AssetManager assetManager) {
        this.name = name;
        this.global = global;
        this.assetManager = assetManager;
    }

    public String run() {
        Context cx = factory.enterContext();
        try (InputStream in = assetManager.open("tests/" + name);
                Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            Scriptable scope = cx.newObject(global);
            scope.setPrototype(global);
            scope.setParentScope(null);
            Object result = cx.evaluateReader(scope, rdr, name, 1, null);
            return ScriptRuntime.toString(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Context.exit();
        }
    }

    @Override
    public String toString() {
        return name;
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
                tests.add(new TestCase(file, global, assetManager));
            }
        }
        return tests;
    }
}
