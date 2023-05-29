package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import java.io.FileReader;
import java.io.IOException;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.shell.Global;

public class ComparatorTest {

    /*
    Check the comparator and ensure that it is consistent.
    An inconsistent comparator will result in an IllegalArgumentException
    with the message "Comparison method violates its general contract!"
    */
    private static void idChk(Object o1, Object o2, int expected) {
        assertEquals(expected, new ScriptableObject.KeyComparator().compare(o1, o2));
        assertEquals(-expected, new ScriptableObject.KeyComparator().compare(o2, o1));
    }

    /*
    This comparator is used only in the generation of object IDs for iteration. It makes sure
    that numeric property names are in numeric order without changing string property names,
    as per the spec.
     */
    @Test
    public void objectIDComparator() {
        idChk(0, 0, 0);
        idChk(1, 0, 1);
        idChk(0, 1, -1);
        idChk("a", "a", 0);
        idChk("b", "a", 0);
        idChk("a", "b", 0);
        idChk("a", 1, 1);
        idChk(1, "a", -1);
    }

    private static void aChk(Object o1, Object o2, int expected) {
        assertEquals(expected, new NativeArray.ElementComparator().compare(o1, o2));
        assertEquals(-expected, new NativeArray.ElementComparator().compare(o2, o1));
    }

    /*
    This comparator is used in Arrays.sort. It ensures that unset properties go at the
    very end, undefined references go right before that, and everything else is sorted as
    a string.
     */
    @Test
    public void arrayComparator() {
        aChk("a", "b", -1);
        aChk("b", "a", 1);
        aChk("a", "a", 0);
        aChk(Scriptable.NOT_FOUND, "a", 1);
        aChk("a", Scriptable.NOT_FOUND, -1);
        aChk(Scriptable.NOT_FOUND, Scriptable.NOT_FOUND, 0);
        aChk(Undefined.instance, "a", 1);
        aChk("a", Undefined.instance, -1);
        aChk(Undefined.instance, Undefined.instance, 0);
        aChk(Undefined.instance, Scriptable.NOT_FOUND, -1);
        aChk(Scriptable.NOT_FOUND, Undefined.instance, 1);
    }

    /*
    Run some tests in JavaScript to verify the custom comparators that are supported
    on Array.sort.
     */
    @Test
    public void customComparator() throws IOException {
        try (Context cx = Context.enter()) {
            Global global = new Global(cx);
            Scriptable root = cx.newObject(global);
            FileReader fr = new FileReader("testsrc/jstests/extensions/custom-comparators.js");

            cx.evaluateReader(root, fr, "custom-comparators.js", 1, null);
        } catch (RhinoException re) {
            System.err.println(re.getScriptStackTrace());
            throw re;
        }
    }
}
