package org.mozilla.javascript.testutils.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.TestSource;

public class TestSourceTest {
    @Test
    public void testPrefixTestFile() {
        String path = TestSource.resolve("testsrc/assert.js");
        assertNotNull(path);
        assertTrue(Files.exists(Path.of(path)));
        assertTrue(Files.isRegularFile(Path.of(path)));
    }

    @Test
    public void testPrefixTestDirectory() {
        String path = TestSource.resolveDirectory("testsrc/assert.js");
        assertNotNull(path);
        assertTrue(Files.exists(Path.of(path)));
        assertTrue(Files.isDirectory(Path.of(path)));
    }

    @Test
    public void testPrefixTestDirectories() {
        String path = TestSource.resolveDirectories("testsrc/assert.js", 2);
        assertNotNull(path);
        assertTrue(Files.exists(Path.of(path)));
        assertTrue(Files.isDirectory(Path.of(path)));
    }
}
