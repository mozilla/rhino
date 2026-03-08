package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.script.ScriptEngineFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests that Rhino modules work correctly under JPMS by creating a ModuleLayer from the built JARs
 * and verifying real module behavior: resolution, service loading, and composition.
 */
public class JpmsModuleTest {

    @Test
    void rhinoExecutesJavaScriptInModuleLayer() throws Exception {
        ModuleLayer layer = createLayer(List.of("rhino"), Set.of("org.mozilla.rhino"));

        ClassLoader loader = layer.findLoader("org.mozilla.rhino");
        Object result = withContextClassLoader(loader, () -> evaluateWithRhino(loader, "1 + 2"));
        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    void scriptEngineDiscoveredViaServiceLoader() throws Exception {
        ModuleLayer layer =
                createLayer(
                        List.of("rhino", "rhino-engine"),
                        Set.of("org.mozilla.rhino", "org.mozilla.rhino.engine"));

        ClassLoader loader = layer.findLoader("org.mozilla.rhino.engine");

        boolean found =
                withContextClassLoader(
                        loader,
                        () -> {
                            for (ScriptEngineFactory factory :
                                    ServiceLoader.load(ScriptEngineFactory.class, loader)) {
                                if (factory.getEngineName().toLowerCase().contains("rhino")) {
                                    Object result = factory.getScriptEngine().eval("40 + 2");
                                    assertEquals(42, ((Number) result).intValue());
                                    return true;
                                }
                            }
                            return false;
                        });
        assertTrue(found, "Rhino ScriptEngineFactory not discovered via ServiceLoader");
    }

    @Test
    void xmlModuleWorksInModuleLayer() throws Exception {
        ModuleLayer layer =
                createLayer(
                        List.of("rhino", "rhino-xml"),
                        Set.of("org.mozilla.rhino", "org.mozilla.javascript.xml"));

        ClassLoader loader = layer.findLoader("org.mozilla.rhino");
        Object result =
                withContextClassLoader(loader, () -> evaluateWithRhino(loader, "typeof XML"));
        assertEquals("function", result);
    }

    @Test
    void xmlModuleNotAvailableWithoutXmlInLayer() throws Exception {
        ModuleLayer layer = createLayer(List.of("rhino"), Set.of("org.mozilla.rhino"));

        ClassLoader loader = layer.findLoader("org.mozilla.rhino");
        Object result =
                withContextClassLoader(loader, () -> evaluateWithRhino(loader, "typeof XML"));
        assertEquals("undefined", result);
    }

    private ModuleLayer createLayer(List<String> jarProperties, Set<String> moduleNames) {
        Path[] jarPaths =
                jarProperties.stream()
                        .map(
                                name -> {
                                    String path = System.getProperty(name + ".jar");
                                    assertNotNull(
                                            path,
                                            "System property '"
                                                    + name
                                                    + ".jar' not set."
                                                    + " Run tests via Gradle.");
                                    return Paths.get(path);
                                })
                        .toArray(Path[]::new);

        ModuleFinder finder = ModuleFinder.of(jarPaths);
        ModuleLayer parent = ModuleLayer.boot();
        Configuration parentConfig = parent.configuration();
        Configuration config = parentConfig.resolve(finder, ModuleFinder.of(), moduleNames);

        return parent.defineModulesWithOneLoader(config, ClassLoader.getPlatformClassLoader());
    }

    private <T> T withContextClassLoader(ClassLoader loader, Callable<T> action) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            return action.call();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private Object evaluateWithRhino(ClassLoader loader, String script) throws Exception {
        Class<?> ctxClass = loader.loadClass("org.mozilla.javascript.Context");
        Object cx = ctxClass.getMethod("enter").invoke(null);
        try {
            Object scope = ctxClass.getMethod("initStandardObjects").invoke(cx);
            return ctxClass.getMethod(
                            "evaluateString",
                            loader.loadClass("org.mozilla.javascript.VarScope"),
                            String.class,
                            String.class,
                            int.class,
                            Object.class)
                    .invoke(cx, scope, script, "test", 1, null);
        } finally {
            ctxClass.getMethod("exit").invoke(null);
        }
    }
}
