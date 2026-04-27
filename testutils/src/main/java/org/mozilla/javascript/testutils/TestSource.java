package org.mozilla.javascript.testutils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class is used to locate the "testsrc" directory and other testing-related resources in the
 * different test environments where Rhino runs.
 */
public class TestSource {
    private final Resolver resolver;

    private static final TestSource SELF = new TestSource();

    private TestSource() {
        resolver = new Resolver();
    }

    /**
     * Return the actual location of the specified file, or throw an AssertionError if the file
     * cannot be found. This method must be used so that file lookup is portable across platforms
     * and build systems, rather than referring directly to files.
     *
     * <p>Typically, "path" in this case will start with either "testsrc" or "test262". This class
     * will locate the "testsrc" directory, which will typically be in a separate location on
     * different modules and build systems.
     */
    public static String resolve(String path) {
        return SELF.resolver.resolve(path);
    }

    /**
     * Return an optional prefix that should be inserted when trying to find files in the "testsrc"
     * directory specifically. This allows scripts to reference files like "testsrc/foo" in all
     * cases and have them resolve.
     *
     * @return the prefix that must be prepended to all file lookups in the "testsrc" directory, or
     *     null if no prefix is necessary.
     */
    public static String getPrefix() {
        return SELF.resolver.getPrefix();
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
     * Like resolveDirectory, but go up "lvl" levels to get the parent of a well-known file multiple
     * levels up.
     */
    public static String resolveDirectories(String path, int lvl) {
        var p = Path.of(SELF.resolver.resolve(path));
        for (int i = 0; p != null && i < lvl; i++) {
            p = p.getParent();
        }
        return p == null ? "" : p.toString();
    }

    /**
     * A class to do the actual resolution. Is is abstracted out to support additional test
     * environments in the future.
     */
    private static class Resolver {
        private final String prefix;

        Resolver() {
            // Look where Gradle will put the files in the "tests" module
            var testSrc = Path.of("./testsrc");
            if (Files.isDirectory(testSrc)) {
                prefix = null;
                return;
            }
            // Look where Bazel will eventually put the files
            testSrc = Path.of("./tests/testsrc");
            if (Files.isDirectory(testSrc)) {
                prefix = "./tests";
                return;
            }
            // Look where Gradle will put the files in other modules
            testSrc = Path.of("../tests/testsrc");
            if (Files.isDirectory(testSrc)) {
                prefix = "../tests";
                return;
            }
            prefix = "NOTFOUND";
        }

        String resolve(String path) {
            if (prefix == null) {
                return path;
            }
            return prefix + '/' + path;
        }

        String getPrefix() {
            return prefix;
        }
    }
}
