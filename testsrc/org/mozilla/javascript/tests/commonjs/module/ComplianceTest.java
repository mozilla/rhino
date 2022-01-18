package org.mozilla.javascript.tests.commonjs.module;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.provider.StrongCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

@RunWith(Parameterized.class)
public class ComplianceTest {

    private File testDir;

    public ComplianceTest(String name, File testDir) {
        this.testDir = testDir;
    }

    @Parameterized.Parameters(name = "/{0}")
    public static Collection<Object[]> data() {
        List<Object[]> retval = new ArrayList<Object[]>(16);
        final File[] files =
                new File("testsrc/org/mozilla/javascript/tests/commonjs/module/1.0").listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                retval.add(new Object[] {file.getName(), file});
            }
        }
        return retval;
    }

    private static Require createRequire(File dir, Context cx, Scriptable scope)
            throws URISyntaxException {
        return new Require(
                cx,
                scope,
                new StrongCachingModuleScriptProvider(
                        new UrlModuleSourceProvider(
                                Collections.singleton(dir.getAbsoluteFile().toURI()),
                                Collections.singleton(
                                        new URI(
                                                ComplianceTest.class
                                                                .getResource(".")
                                                                .toExternalForm()
                                                        + "/")))),
                null,
                null,
                false);
    }

    @org.junit.Test
    public void testRequire() throws Throwable {
        final Context cx = Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            final Scriptable scope = cx.initStandardObjects();
            ScriptableObject.putProperty(scope, "print", new Print(scope));
            createRequire(testDir, cx, scope).requireMain(cx, "program");
        } finally {
            Context.exit();
        }
    }

    private static class Print extends ScriptableObject implements Function {
        Print(Scriptable scope) {
            setPrototype(ScriptableObject.getFunctionPrototype(scope));
        }

        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length > 1 && "fail".equals(args[1])) {
                throw new AssertionFailedError(String.valueOf(args[0]));
            }
            return null;
        }

        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            throw new AssertionFailedError("Shouldn't be invoked as constructor");
        }

        @Override
        public String getClassName() {
            return "Function";
        }
    }
}
