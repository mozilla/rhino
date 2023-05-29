package org.mozilla.javascript.tests.harmony;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;
import org.mozilla.javascript.typedarrays.NativeFloat32Array;
import org.mozilla.javascript.typedarrays.NativeFloat64Array;
import org.mozilla.javascript.typedarrays.NativeInt16Array;
import org.mozilla.javascript.typedarrays.NativeInt32Array;
import org.mozilla.javascript.typedarrays.NativeInt8Array;
import org.mozilla.javascript.typedarrays.NativeUint16Array;
import org.mozilla.javascript.typedarrays.NativeUint32Array;
import org.mozilla.javascript.typedarrays.NativeUint8Array;
import org.mozilla.javascript.typedarrays.NativeUint8ClampedArray;

/** Ensure that the "List" contract is valid for a typed array. */
public class TypedArrayJavaTest {
    @Test
    public void int8() {
        NativeInt8Array a = new NativeInt8Array(2);
        testTwoList(a, (byte) 1, (byte) 2, (byte) 3);
    }

    @Test
    public void int8Equals() {
        byte a = 3;
        byte b = 4;

        NativeInt8Array list = new NativeInt8Array(new NativeArrayBuffer(2), 0, 2);
        list.set(0, a);
        list.set(1, b);

        List<Byte> gl = new NativeInt8Array(new NativeArrayBuffer(2), 0, 2);
        gl.set(0, a);
        gl.set(1, b);
        assertTrue(gl.equals(list));
        assertEquals(gl.hashCode(), list.hashCode());
        assertFalse(gl.equals(a));

        List<Byte> bl = new NativeInt8Array(new NativeArrayBuffer(2), 0, 2);
        bl.set(0, b);
        bl.set(1, a);
        assertFalse(bl.equals(list));

        Byte[] ba = new Byte[2];
        ba[0] = a;
        ba[1] = b;

        Byte[] na1 = list.toArray(new Byte[0]);
        assertArrayEquals(ba, na1);
        Byte[] ta2 = new Byte[2];
        Byte[] na2 = list.toArray(ta2);
        assertArrayEquals(ba, na2);
        assertTrue(na2 == ta2);
        Byte[] ta3 = new Byte[4];
        Byte[] na3 = list.toArray(ta3);
        assertFalse(na3.equals(list));
        assertTrue(na3 == ta3);
        assertEquals((Byte) a, na3[0]);
        assertEquals((Byte) b, na3[1]);

        try {
            list.toArray(new String[2]);
            assertTrue("Expected exception", false);
        } catch (ArrayStoreException ae) {
        }
    }

    @Test
    public void uInt8() {
        NativeUint8Array a = new NativeUint8Array(2);
        testTwoList(a, 1, 2, 3);
    }

    @Test
    public void uInt8Clamped() {
        NativeUint8ClampedArray a = new NativeUint8ClampedArray(2);
        testTwoList(a, 1, 2, 3);
    }

    @Test
    public void int16() {
        NativeInt16Array a = new NativeInt16Array(2);
        testTwoList(a, (short) 1, (short) 2, (short) 3);
    }

    @Test
    public void uInt16() {
        NativeUint16Array a = new NativeUint16Array(2);
        testTwoList(a, 1, 2, 3);
    }

    @Test
    public void int32() {
        NativeInt32Array a = new NativeInt32Array(2);
        testTwoList(a, 1, 2, 3);
    }

    @Test
    public void uInt32() {
        NativeUint32Array a = new NativeUint32Array(2);
        testTwoList(a, 1L, 2L, 3L);
    }

    @Test
    public void float32() {
        NativeFloat32Array a = new NativeFloat32Array(2);
        testTwoList(a, 1.0f, 2.0f, 3.0F);
    }

    @Test
    public void float64() {
        NativeFloat64Array a = new NativeFloat64Array(2);
        testTwoList(a, 1.0, 2.0, 3.0);
    }

    private static <T> void testTwoList(List<T> list, T a, T b, T bogus) {
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
        list.set(0, a);
        assertEquals(a, list.get(0));
        list.set(1, b);
        assertEquals(b, list.get(1));

        try {
            list.get(3);
            assertFalse("Exception expected", true);
        } catch (IndexOutOfBoundsException ie) {
        }
        try {
            list.set(-1, a);
            assertFalse("Exception expected", true);
        } catch (IndexOutOfBoundsException ie) {
        }
        try {
            list.add(a);
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            list.remove(0);
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            list.remove(a);
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            list.removeAll(Arrays.asList(a, b));
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            list.retainAll(Arrays.asList(a, b));
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            list.add(0, a);
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            Collection<T> empty = Collections.emptyList();
            list.addAll(empty);
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            Collection<T> empty = Collections.emptyList();
            list.addAll(0, empty);
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            list.subList(0, 1);
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }
        try {
            list.clear();
            assertFalse("Exception expected", true);
        } catch (UnsupportedOperationException uoe) {
        }

        assertTrue(list.contains(a));
        assertFalse(list.contains(bogus));
        assertTrue(list.containsAll(Arrays.asList(a, b)));
        assertFalse(list.containsAll(Arrays.asList(a, b, bogus)));
        assertEquals(0, list.indexOf(a));
        assertEquals(1, list.indexOf(b));
        assertEquals(-1, list.indexOf(bogus));
        assertEquals(0, list.lastIndexOf(a));
        assertEquals(1, list.lastIndexOf(b));
        assertEquals(-1, list.lastIndexOf(bogus));

        Object[] a1 = list.toArray();
        assertEquals(a1[0], a);
        assertEquals(a1[1], b);

        Iterator<T> i = list.iterator();
        assertTrue(i.hasNext());
        assertEquals(a, i.next());
        assertEquals(b, i.next());
        assertFalse(i.hasNext());

        ListIterator<T> l = list.listIterator();
        assertTrue(l.hasNext());
        assertFalse(l.hasPrevious());
        assertEquals(0, l.nextIndex());
        assertEquals(-1, l.previousIndex());
        assertEquals(a, l.next());
        assertEquals(b, l.next());
        assertTrue(l.hasPrevious());
        assertFalse(l.hasNext());
        assertEquals(2, l.nextIndex());
        assertEquals(1, l.previousIndex());
        assertEquals(b, l.previous());
        assertEquals(a, l.previous());

        ListIterator<T> l1 = list.listIterator(1);
        assertTrue(l1.hasNext());
        assertTrue(l1.hasPrevious());
        assertEquals(b, l1.next());
        assertTrue(l1.hasPrevious());
        assertFalse(l1.hasNext());
        assertEquals(b, l1.previous());
        assertEquals(a, l1.previous());

        ListIterator<T> l2 = list.listIterator();

        try {
            l2.set(bogus);
            assertTrue("Expected exception", false);
        } catch (IllegalStateException e) {
        }

        assertEquals(a, l2.next());
        l2.set(bogus);
        assertEquals(bogus, list.get(0));
        assertEquals(b, l2.next());
        assertEquals(b, l2.previous());
        l2.set(bogus);
        assertEquals(bogus, list.get(1));
        assertEquals(bogus, l2.previous());

        try {
            l2.remove();
            assertFalse("Expected exception", true);
        } catch (UnsupportedOperationException e) {
        }

        try {
            l2.add(bogus);
            assertFalse("Expected exception", true);
        } catch (UnsupportedOperationException e) {
        }
    }

    /**
     * Test case for {@link https://github.com/mozilla/rhino/issues/449}
     *
     * @throws Exception if test failed
     */
    @Test
    public void getAllIds() throws Exception {
        String[] allNativeTypes = {
            "Float32Array",
            "Float64Array",
            "Int8Array",
            "Int16Array",
            "Int32Array",
            "Uint8Array",
            "Uint16Array",
            "Uint32Array",
            "Uint8ClampedArray"
        };

        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable global = cx.initStandardObjects();

            for (String type : allNativeTypes) {
                ScriptableObject obj =
                        (ScriptableObject)
                                cx.evaluateString(global, "new " + type + "(5)", "", 1, null);
                obj.getAllIds();
            }
        }
    }
}
