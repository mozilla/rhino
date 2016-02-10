package org.mozilla.javascript.tests;

import junit.framework.TestCase;
import org.mozilla.javascript.ConsString;

public class ConsStringTest extends TestCase {
    public void testAppend() {
        ConsString current = new ConsString("a", "b");
        current = new ConsString(current, "c");
        current = new ConsString(current, "d");

        assertEquals("abcd", current.toString());
    }

    public void testAppendManyStrings() {
        ConsString current = new ConsString("a", "a");
        for(int i = 0; i < 1000000; i++) {
            current = new ConsString(current, "a");
        }
        assertNotNull(current.toString());
    }

    public void testAppendManyStringsRecursive() {
        recurseAndAppend(4000);
    }

    private void recurseAndAppend(int depth) {
        if (depth == 0) {
            ConsString current = new ConsString("a", "a");
            for(int i = 0; i < 1000000; i++) {
                current = new ConsString(current, "a");
            }
            assertNotNull(current.toString());
        } else {
            recurseAndAppend(depth-1);
        }
    }
}
