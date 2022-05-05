package org.mozilla.javascript.tests;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;

public class HashCollisionTest {
    private static final String mediumInput = "testsrc/jstests/collisions.json";
    private static String collisions;

    @BeforeClass
    public static void loadFile() throws IOException {
        StringWriter out = new StringWriter();
        FileReader in = new FileReader(mediumInput);

        try {
            char[] buf = new char[16392];
            int rc;
            do {
                rc = in.read(buf);
                if (rc > 0) {
                    out.write(buf, 0, rc);
                }
            } while (rc > 0);

            collisions = out.toString();

        } finally {
            in.close();
        }
    }

    /**
     * This test loads a file full of keys that collide with the standard
     * java.lang.String.hashCode() method. Without a collision-resistant hash table, this test takes
     * over two minutes to execute.
     */
    @Test
    public void testMediumCollisions() throws IOException {
        FileReader scriptIn = new FileReader("testsrc/jstests/hash-collisions.js");
        Context cx = Context.enter();
        try {
            Global glob = new Global(cx);
            glob.put("collisions", glob, collisions);
            cx.evaluateReader(glob, scriptIn, "hash-collisons.js", 1, null);
        } finally {
            Context.exit();
            scriptIn.close();
        }
    }
}
