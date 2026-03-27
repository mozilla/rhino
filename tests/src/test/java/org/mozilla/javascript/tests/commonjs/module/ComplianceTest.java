package org.mozilla.javascript.tests.commonjs.module;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.provider.StrongCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.mozilla.javascript.testutils.Utils;
import org.opentest4j.AssertionFailedError;

public class ComplianceTest {

    private File testDir;

    public void initComplianceTest(String name, File testDir) {
        this.testDir = testDir;
    }

    public static Collection<Object[]> data() {
        List<Object[]> retval = new ArrayList<Object[]>(16);
        final File[] files =
                new File("src/test/resources/org/mozilla/javascript/tests/commonjs/module/1.0")
                        .listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                retval.add(new Object[] {file.getName(), file});
            }
        }
        return retval;
    }

    private static Require createRequire(File dir, Context cx, TopLevel scope)
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

    @MethodSource("data")
    @ParameterizedTest(name = "/{0}")
    public void require(String name, File testDir) throws Throwable {
        initComplianceTest(name, testDir);
        Utils.runWithAllModes(
                cx -> {
                    final TopLevel scope = cx.initStandardObjects();
                    ScriptableObject.putProperty(scope, "print", new Print(scope));
                    try {
                        createRequire(testDir, cx, scope).requireMain(cx, "program");
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }

                    return null;
                });
    }

    private static class Print extends ScriptableObject implements Function {
        Print(Scriptable scope) {
            setPrototype(ScriptableObject.getFunctionPrototype(scope));
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length > 1 && "fail".equals(args[1])) {
                throw new AssertionFailedError(String.valueOf(args[0]));
            }
            return null;
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            throw new AssertionFailedError("Shouldn't be invoked as constructor");
        }

        @Override
        public String getClassName() {
            return "Function";
        }
    }
}
