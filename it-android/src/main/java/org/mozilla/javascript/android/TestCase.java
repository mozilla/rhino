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
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class TestCase {

    private final String name;
    private final Scriptable global;
    private final AssetManager assetManager;

    public TestCase(String name, Scriptable global, AssetManager assetManager) {
        this.name = name;
        this.global = global;
        this.assetManager = assetManager;
    }

    public String run() {
        try (InputStream in = assetManager.open("tests/" + name);
                Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
                Context cx = Context.enter()) {
            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            cx.setGeneratingDebug(true);
            Scriptable scope = cx.newObject(global);
            scope.setParentScope(null);
            Object result = cx.evaluateReader(global, rdr, name, 1, null);
            return ScriptRuntime.toString(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
        try (InputStream in = assetManager.open("assert.js");
                Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
                Context cx = Context.enter()) {
            cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
            cx.setGeneratingDebug(true);
            global = cx.initStandardObjects();
            cx.evaluateReader(global, rdr, "assert.js", 1, null);
            // TODO: Does not work yet on android: global.sealObject();
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
