package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Tests that Rhino bundles work correctly in an OSGi environment by embedding an Apache Felix
 * framework, installing the built JARs as bundles, and verifying real behavior. The Apache Aries
 * SPI Fly "dynamic" bundle is installed and started first so that {@code ServiceLoader.load} calls
 * inside the Rhino bundles resolve against the bundle's own {@code osgi.serviceloader} providers
 * rather than failing due to thread-context-classloader visibility.
 */
public class OsgiModuleTest {

    @Test
    void rhinoBundleResolvesAndStarts() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    rhino.start();
                    assertEquals(Bundle.ACTIVE, rhino.getState());
                });
    }

    @Test
    void rhinoExecutesJavaScriptInOsgi() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    rhino.start();

                    Object result = evaluateInBundle(rhino, "1 + 2");
                    assertEquals(3, ((Number) result).intValue());
                });
    }

    @Test
    void rhinoExecutesRegExpJavaScriptInOsgi() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    rhino.start();

                    Object result = evaluateInBundle(rhino, "/foo/.test('foo')");
                    assertTrue((Boolean) result);
                });
    }

    @Test
    void rhinoEngineBundleResolvesAndStarts() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    Bundle engine = installBundle(ctx, "rhino-engine");
                    rhino.start();
                    engine.start();
                    assertEquals(Bundle.ACTIVE, engine.getState());

                    // Verify the ScriptEngineFactory class is loadable from the bundle
                    Class<?> factory =
                            engine.loadClass(
                                    "org.mozilla.javascript.engine.RhinoScriptEngineFactory");
                    assertNotNull(factory);
                });
    }

    @Test
    void rhinoToolsBundleResolvesAndStarts() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    Bundle tools = installBundle(ctx, "rhino-tools");
                    rhino.start();
                    tools.start();
                    assertEquals(Bundle.ACTIVE, tools.getState());
                });
    }

    @Test
    void rhinoXmlBundleResolvesAndStarts() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    Bundle xml = installBundle(ctx, "rhino-xml");
                    rhino.start();
                    xml.start();
                    assertEquals(Bundle.ACTIVE, xml.getState());
                });
    }

    @Test
    void allBundlesResolveAndStartTogether() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    Bundle engine = installBundle(ctx, "rhino-engine");
                    Bundle tools = installBundle(ctx, "rhino-tools");
                    Bundle xml = installBundle(ctx, "rhino-xml");

                    rhino.start();
                    engine.start();
                    tools.start();
                    xml.start();

                    assertEquals(Bundle.ACTIVE, rhino.getState(), "rhino");
                    assertEquals(Bundle.ACTIVE, engine.getState(), "rhino-engine");
                    assertEquals(Bundle.ACTIVE, tools.getState(), "rhino-tools");
                    assertEquals(Bundle.ACTIVE, xml.getState(), "rhino-xml");
                });
    }

    /**
     * Verifies that rhino-xml's exported class is accessible from the bundle classloader, i.e. the
     * OSGi wiring between rhino and rhino-xml is correct. Full cross-bundle ServiceLoader discovery
     * (making {@code typeof XML === "function"}) requires an OSGi service loader mediator like SPI
     * Fly, which is beyond the scope of this test.
     */
    @Test
    void xmlBundleExportsAreAccessible() throws Exception {
        withFramework(
                ctx -> {
                    Bundle rhino = installBundle(ctx, "rhino");
                    Bundle xml = installBundle(ctx, "rhino-xml");
                    rhino.start();
                    xml.start();

                    Class<?> loaderImpl =
                            xml.loadClass("org.mozilla.javascript.xmlimpl.XMLLoaderImpl");
                    assertNotNull(loaderImpl);
                    Class<?> loaderInterface =
                            rhino.loadClass("org.mozilla.javascript.xml.XMLLoader");
                    assertTrue(
                            loaderInterface.isAssignableFrom(loaderImpl),
                            "XMLLoaderImpl should implement XMLLoader");
                });
    }

    /**
     * Evaluates a JavaScript expression using Rhino classes loaded from the given bundle. The
     * thread context classloader is left untouched; SPI Fly's weaver rewrites {@code
     * ServiceLoader.load} calls inside the Rhino bundle to use the bundle's own {@code
     * osgi.serviceloader} providers.
     */
    private Object evaluateInBundle(Bundle bundle, String script) throws Exception {
        Class<?> ctxClass = bundle.loadClass("org.mozilla.javascript.Context");
        Object cx = ctxClass.getMethod("enter").invoke(null);
        try {
            Object scope = ctxClass.getMethod("initStandardObjects").invoke(cx);
            return ctxClass.getMethod(
                            "evaluateString",
                            bundle.loadClass("org.mozilla.javascript.VarScope"),
                            String.class,
                            String.class,
                            int.class,
                            Object.class)
                    .invoke(cx, scope, script, "test", 1, null);
        } finally {
            ctxClass.getMethod("exit").invoke(null);
        }
    }

    // --- Helpers ---

    private void withFramework(FrameworkConsumer action) throws Exception {
        Path storageDir = Files.createTempDirectory("osgi-test");
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE, storageDir.toString());
        config.put(
                Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        // Boot-delegate jdk.dynalink so bundles can access it without importing
        config.put(Constants.FRAMEWORK_BOOTDELEGATION, "jdk.dynalink,jdk.dynalink.*");

        FrameworkFactory factory =
                java.util.ServiceLoader.load(FrameworkFactory.class)
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalStateException("No OSGi FrameworkFactory found"));

        Framework framework = factory.newFramework(config);
        framework.start();
        try {
            BundleContext ctx = framework.getBundleContext();
            // Install and start the extra OSGi integration bundles (SPI Fly + ASM) first so the
            // Service Loader Mediator weaving hook is active before any Rhino bundle class is
            // loaded.
            installOsgiIntegrationBundles(ctx);
            action.accept(ctx);
        } finally {
            framework.stop();
            framework.waitForStop(10_000);
        }
    }

    private void installOsgiIntegrationBundles(BundleContext ctx) throws Exception {
        String jars = System.getProperty("osgi.integration.bundles");
        assertTrue(
                jars != null && !jars.isEmpty(),
                "System property 'osgi.integration.bundles' not set. Run tests via Gradle.");
        List<Bundle> installed = new ArrayList<>();
        for (String jar : jars.split(",")) {
            installed.add(ctx.installBundle("file:" + jar));
        }
        for (Bundle b : installed) {
            if (b.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                b.start();
            }
        }
    }

    private Bundle installBundle(BundleContext ctx, String projectName) throws Exception {
        String path = System.getProperty(projectName + ".jar");
        assertTrue(
                path != null && !path.isEmpty(),
                "System property '" + projectName + ".jar' not set. Run tests via Gradle.");
        return ctx.installBundle("file:" + path);
    }

    @FunctionalInterface
    private interface FrameworkConsumer {
        void accept(BundleContext ctx) throws Exception;
    }
}
