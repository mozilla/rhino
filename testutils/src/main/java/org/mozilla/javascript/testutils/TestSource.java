package org.mozilla.javascript.testutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * This class is used to locate the "testsrc" directory and other testing-related resources in the
 * different test environments where Rhino runs.
 */
public class TestSource {
    private final Resolver resolver;

    private static final TestSource SELF = new TestSource();

    private TestSource() {
        if (BazelResolver.isBazel()) {
            resolver = new BazelResolver();
        } else {
            resolver = new NativeResolver();
        }
    }

    /**
     * Return the actual location of the specified file, or throw an AssertionError if the file
     * cannot be found. This method must be used so that file lookup is portable across platforms
     * and build systems.
     */
    public static String resolve(String path) {
        return SELF.resolver.resolve(path);
    }

    /**
     * Return the parent directory of the specified file, or throw an AssertionError if the file
     * cannot be found. Because of the way that Bazel works, this is preferable to another way.
     * Callers should use this method by resolving a well-known file, and then this method will
     * return the directory that contains the file. The directory may then be safely traversed to
     * find other files that are known to resolve there.
     */
    public static String resolveDirectory(String path) {
        return Path.of(SELF.resolver.resolve(path)).getParent().toString();
    }

    /**
     * Return the prefix that should be added to methods like "load" so that they can put this
     * before a file name like "testsrc/assert.js" and end up in the right place.
     */
    public static String getPrefix() {
        return SELF.resolver.getPrefix();
    }

    private abstract static class Resolver {
        abstract String resolve(String path);

        abstract String getPrefix();
    }

    private static class NativeResolver extends Resolver {
        private final String prefix;

        NativeResolver() {
            var testSrc = Path.of("./testsrc");
            if (Files.isDirectory(testSrc)) {
                prefix = null;
                return;
            }
            testSrc = Path.of("./tests/testsrc");
            if (Files.isDirectory(testSrc)) {
                prefix = "./tests";
                return;
            }
            testSrc = Path.of("../tests/testsrc");
            if (Files.isDirectory(testSrc)) {
                prefix = "../tests";
                return;
            }
            prefix = "NOTFOUND";
        }

        @Override
        String resolve(String path) {
            if (prefix == null) {
                return path;
            }
            return prefix + '/' + path;
        }

        @Override
        String getPrefix() {
            return prefix;
        }
    }

    private static class BazelResolver extends Resolver {
        private static final Pattern WS = Pattern.compile("\\s");

        private final HashMap<String, String> manifest = new HashMap<>();
        private final String workspace;

        static boolean isBazel() {
            var mani = System.getenv("RUNFILES_MANIFEST_FILE");
            if (mani == null) {
                return false;
            }
            return Files.isReadable(Path.of(mani));
        }

        BazelResolver() {
            String ws = System.getenv("TEST_WORKSPACE");
            workspace = ws != null ? ws : "";
            String maniPath = System.getenv("RUNFILES_MANIFEST_FILE");
            assert maniPath != null;
            try {
                for (String line : Files.readAllLines(Path.of(maniPath))) {
                    String[] parts = WS.split(line, 2);
                    if (parts.length == 2) {
                        manifest.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading Bazel manifest file: " + e);
            }
        }

        @Override
        String resolve(String path) {
            // Handle cases where the test directory might be in other places
            String p = manifest.get(workspace + '/' + path);
            if (p == null) {
                p = manifest.get(workspace + "/tests/" + path);
            }
            if (p == null) {
                throw new AssertionError("Can't find test file " + path);
            }
            return p;
        }

        @Override
        String getPrefix() {
            // Find a well-known file and go up from there
            var p = Path.of(resolve("testsrc/assert.js"));
            p = p.getParent();
            if (p != null) {
                p = p.getParent();
            }
            return p == null ? null : p.toString();
        }
    }
}
