package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Provides;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Validate OSGi metadata for completeness in relation to module-info.java content. */
public class OsgiMetadataTest {

    private static final String SERVICELOADER_PROCESSOR =
            "osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.processor)\"";

    private static final String SERVICELOADER_REGISTRAR =
            "osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.registrar)\"";

    @Test
    void testRhinoOsgiMetadata() throws Exception {
        verifyOsgiMetadata(org.mozilla.javascript.Context.class);
    }

    @Test
    void testRhinoEngineOsgiMetadata() throws Exception {
        verifyOsgiMetadata(org.mozilla.javascript.engine.RhinoScriptEngineFactory.class);
    }

    @Test
    void testRhinoToolsOsgiMetadata() throws Exception {
        verifyOsgiMetadata(org.mozilla.javascript.tools.shell.Main.class);
    }

    @Test
    void testRhinoXmlOsgiMetadata() throws Exception {
        verifyOsgiMetadata(org.mozilla.javascript.xmlimpl.XMLLoaderImpl.class);
    }

    private void verifyOsgiMetadata(Class<?> representativeClass) throws Exception {
        URL location = representativeClass.getProtectionDomain().getCodeSource().getLocation();
        Path path = Paths.get(location.toURI());

        ModuleDescriptor descriptor;
        Manifest manifest;

        if (Files.isDirectory(path)) {
            try (InputStream is = Files.newInputStream(path.resolve("module-info.class"))) {
                descriptor = ModuleDescriptor.read(is);
            }
            try (InputStream is = Files.newInputStream(path.resolve("META-INF/MANIFEST.MF"))) {
                manifest = new Manifest(is);
            }
        } else {
            try (JarFile jar = new JarFile(path.toFile())) {
                try (InputStream is = jar.getInputStream(jar.getEntry("module-info.class"))) {
                    descriptor = ModuleDescriptor.read(is);
                }
                manifest = jar.getManifest();
            }
        }

        assertAll(
                () -> verifyExports(descriptor, manifest),
                () -> verifyUses(descriptor, manifest),
                () -> verifyProvides(descriptor, manifest));
    }

    /** Every <code>exports</code> module declaration must have appropriate Export-Package. */
    private void verifyExports(ModuleDescriptor descriptor, Manifest manifest) {
        String osgiExportHeader = manifest.getMainAttributes().getValue("Export-Package");
        if (osgiExportHeader == null) {
            assertTrue(descriptor.exports().isEmpty(), "Missing OSGi Export-Package header");
            return;
        }

        List<String> osgiExportValues = splitOsgiHeader(osgiExportHeader);

        for (Exports exports : descriptor.exports()) {
            String pattern = Pattern.quote(exports.source()) + "(;.*|$)";
            assertTrue(
                    osgiExportValues.stream().anyMatch(it -> it.matches(pattern)),
                    "Missing OSGi Export-Package for " + exports.source());
        }

        assertEquals(
                descriptor.exports().size(),
                osgiExportValues.size(),
                "Excessive OSGi Export-Package declarations");
    }

    /**
     * Every <code>uses</code> module declaration must have appropriate Require-Capability for
     * Service Loader.
     */
    private void verifyUses(ModuleDescriptor descriptor, Manifest manifest) {
        String osgiRequireHeader = manifest.getMainAttributes().getValue("Require-Capability");
        if (osgiRequireHeader == null) {
            assertTrue(
                    descriptor.uses().isEmpty(),
                    "Missing OSGi Require-Capability header for Service Loader");
            return;
        }

        List<String> osgiRequireValues = splitOsgiHeader(osgiRequireHeader);

        assertTrue(
                descriptor.uses().isEmpty() || osgiRequireValues.contains(SERVICELOADER_PROCESSOR),
                "Missing OSGi Require-Capability for osgi.serviceloader.processor extender definition");

        List<String> osgiLoaderRequires =
                osgiRequireValues.stream()
                        .filter(it -> it.startsWith("osgi.serviceloader;"))
                        .filter(
                                it ->
                                        !it.startsWith(
                                                "osgi.serviceloader=javax.script.ScriptEngineFactory"))
                        .collect(Collectors.toList());

        for (String uses : descriptor.uses()) {
            String pattern =
                    "osgi.serviceloader;filter:=\"\\(osgi.serviceloader="
                            + Pattern.quote(uses)
                            + "\\)\"(;.*|$)";
            assertTrue(
                    osgiLoaderRequires.stream().anyMatch(it -> it.matches(pattern)),
                    "Missing OSGi Require-Capability Service Loader requirement for " + uses);
        }

        assertEquals(
                descriptor.uses().size(),
                osgiLoaderRequires.size(),
                "Excessive OSGi Require-Capability Service Loader declarations");
    }

    /**
     * Every <code>provides</code> module declaration must have appropriate Provide-Capability for
     * Service Loader.
     */
    private void verifyProvides(ModuleDescriptor descriptor, Manifest manifest) {
        String osgiRequireHeader = manifest.getMainAttributes().getValue("Require-Capability");

        String osgiProvideHeader = manifest.getMainAttributes().getValue("Provide-Capability");
        if (osgiProvideHeader == null) {
            assertTrue(
                    descriptor.provides().isEmpty(),
                    "Missing OSGi Provide-Capability header for Service Loader");
            assertFalse(
                    osgiRequireHeader != null
                            && splitOsgiHeader(osgiRequireHeader).contains(SERVICELOADER_REGISTRAR),
                    "Useless OSGi Require-Capability osgi.serviceloader.processor extender definition");
            return;
        }

        assertTrue(
                osgiRequireHeader != null
                        && splitOsgiHeader(osgiRequireHeader).contains(SERVICELOADER_REGISTRAR),
                "Missing OSGi Require-Capability osgi.serviceloader.registrar extender definition");

        List<String> osgiProvideValues = splitOsgiHeader(osgiProvideHeader);

        List<String> osgiLoaderProvides =
                osgiProvideValues.stream()
                        .filter(it -> it.startsWith("osgi.serviceloader;"))
                        .collect(Collectors.toList());

        for (Provides provides : descriptor.provides()) {
            String pattern =
                    "osgi.serviceloader;osgi.serviceloader=\""
                            + Pattern.quote(provides.service())
                            + "\"";
            assertTrue(
                    osgiLoaderProvides.stream().anyMatch(it -> it.matches(pattern)),
                    "Missing OSGi Provide-Capability Service Loader for " + provides.service());
        }

        assertEquals(
                descriptor.provides().size(),
                osgiLoaderProvides.size(),
                "Excessive OSGi Provide-Capability Service Loader declarations");
    }

    private List<String> splitOsgiHeader(String header) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < header.length(); i++) {
            char c = header.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        return parts;
    }
}
