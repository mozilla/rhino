package org.mozilla.javascript.tests.commonjs.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.provider.StrongCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

/**
 * @author Attila Szegedi
 * @version $Id: RequireTest.java,v 1.1 2011/04/07 22:24:37 hannes%helma.at Exp $
 */
public class RequireTest {

    @Test
    public void sandboxed() throws Exception {
        try (Context cx = createContext()) {
            final Require require = getSandboxedRequire(cx);
            require.requireMain(cx, "testSandboxed");
            // Also, test idempotent double-require of same main:
            require.requireMain(cx, "testSandboxed");
            // Also, test failed require of different main:
            try {
                require.requireMain(cx, "blah");
                fail();
            } catch (IllegalStateException e) {
                // Expected, success
            }
        }
    }

    private static Context createContext() {
        final Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        return cx;
    }

    @Test
    public void nonSandboxed() throws Exception {
        try (Context cx = createContext()) {
            final Scriptable scope = cx.initStandardObjects();
            final Require require = getSandboxedRequire(cx, scope, false);
            final String jsFile = getClass().getResource("testNonSandboxed.js").toExternalForm();
            ScriptableObject.putProperty(scope, "moduleUri", jsFile);
            require.requireMain(cx, "testNonSandboxed");
        }
    }

    public static class CustomGlobal extends ScriptableObject {
        public int jsFunction_test(int x) {
            return x + 1;
        }

        @Override
        public String getClassName() {
            return "CustomGlobal";
        }
    }

    @Test
    public void customGlobal() throws Exception {
        try (Context cx = createContext()) {
            final Scriptable scope = cx.initStandardObjects();
            ScriptableObject.defineClass(scope, CustomGlobal.class);

            final Scriptable global = cx.newObject(scope, "CustomGlobal", null);

            global.getPrototype().setPrototype(scope);
            global.setParentScope(null);

            final Require require =
                    new Require(
                            cx,
                            global,
                            new StrongCachingModuleScriptProvider(
                                    new UrlModuleSourceProvider(
                                            Collections.singleton(getDirectory()), null)),
                            null,
                            null,
                            true);

            require.install(global);

            try {
                cx.evaluateReader(
                        global, getReader("testCustomGlobal.js"), "testCustomGlobal.js", 1, null);
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                throw ex;
            }
        }
    }

    @Test
    public void variousUsageErrors() throws Exception {
        testWithSandboxedRequire("testNoArgsRequire");
    }

    @Test
    public void relativeId() throws Exception {
        try (Context cx = createContext()) {
            final Scriptable scope = cx.initStandardObjects();
            final Require require = getSandboxedRequire(cx, scope, false);
            require.install(scope);
            cx.evaluateReader(scope, getReader("testRelativeId.js"), "testRelativeId.js", 1, null);
        }
    }

    @Test
    public void setMainForAlreadyLoadedModule() throws Exception {
        try (Context cx = createContext()) {
            final Scriptable scope = cx.initStandardObjects();
            final Require require = getSandboxedRequire(cx, scope, false);
            require.install(scope);
            cx.evaluateReader(
                    scope,
                    getReader("testSetMainForAlreadyLoadedModule.js"),
                    "testSetMainForAlreadyLoadedModule.js",
                    1,
                    null);
            try {
                require.requireMain(cx, "assert");
                fail();
            } catch (IllegalStateException e) {
                assertEquals(e.getMessage(), "Attempt to set main module after it was loaded");
            }
        }
    }

    private Reader getReader(String name) {
        return new InputStreamReader(getClass().getResourceAsStream(name));
    }

    private void testWithSandboxedRequire(String moduleId) throws Exception {
        try (Context cx = createContext()) {
            getSandboxedRequire(cx).requireMain(cx, moduleId);
        }
    }

    private Require getSandboxedRequire(final Context cx) throws URISyntaxException {
        return getSandboxedRequire(cx, cx.initStandardObjects(), true);
    }

    private Require getSandboxedRequire(Context cx, Scriptable scope, boolean sandboxed)
            throws URISyntaxException {
        return new Require(
                cx,
                cx.initStandardObjects(),
                new StrongCachingModuleScriptProvider(
                        new UrlModuleSourceProvider(Collections.singleton(getDirectory()), null)),
                null,
                null,
                true);
    }

    private URI getDirectory() throws URISyntaxException {
        final String jsFile = getClass().getResource("testSandboxed.js").toExternalForm();
        return new URI(jsFile.substring(0, jsFile.lastIndexOf('/') + 1));
    }
}
