/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptableObject;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=466207
 *
 * @author Hannes Wallnoefer
 */
public class Bug466207Test {

    List<Object> list, reference;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        // set up a reference map
        reference = new ArrayList<Object>();
        reference.add("a");
        reference.add(Boolean.TRUE);
        reference.add(new HashMap<Object, Object>());
        reference.add(Integer.valueOf(42));
        reference.add("a");
        // get a js object as map

        try (Context context = Context.enter()) {
            ScriptableObject scope = context.initStandardObjects();
            list =
                    (List<Object>)
                            context.evaluateString(
                                    scope,
                                    "(['a', true, new java.util.HashMap(), 42, 'a']);",
                                    "testsrc",
                                    1,
                                    null);
        }
    }

    @Test
    public void testEqual() {
        // FIXME we do not override equals() and hashCode() in NativeArray
        // so calling this with swapped argument fails. This breaks symmetry
        // of equals(), but overriding these methods might be risky.
        assertEquals(reference, list);
    }

    @Test
    public void indexedAccess() {
        assertTrue(list.size() == 5);
        assertEquals(list.get(0), reference.get(0));
        assertEquals(list.get(1), reference.get(1));
        assertEquals(list.get(2), reference.get(2));
        assertEquals(list.get(3), reference.get(3));
        assertEquals(list.get(4), reference.get(4));
    }

    @Test
    public void contains() {
        assertTrue(list.contains("a"));
        assertTrue(list.contains(Boolean.TRUE));
        assertFalse(list.contains("x"));
        assertFalse(list.contains(Boolean.FALSE));
        assertFalse(list.contains(null));
    }

    @Test
    public void indexOf() {
        assertTrue(list.indexOf("a") == 0);
        assertTrue(list.indexOf(Boolean.TRUE) == 1);
        assertTrue(list.lastIndexOf("a") == 4);
        assertTrue(list.lastIndexOf(Boolean.TRUE) == 1);
        assertTrue(list.indexOf("x") == -1);
        assertTrue(list.lastIndexOf("x") == -1);
        assertTrue(list.indexOf(null) == -1);
        assertTrue(list.lastIndexOf(null) == -1);
    }

    @Test
    public void toArray() {
        assertTrue(Arrays.equals(list.toArray(), reference.toArray()));
        assertTrue(Arrays.equals(list.toArray(new Object[5]), reference.toArray(new Object[5])));
        assertTrue(Arrays.equals(list.toArray(new Object[6]), reference.toArray(new Object[6])));
    }

    @Test
    public void iterator() {
        compareIterators(list.iterator(), reference.iterator());
        compareIterators(list.listIterator(), reference.listIterator());
        compareIterators(list.listIterator(2), reference.listIterator(2));
        compareIterators(list.listIterator(3), reference.listIterator(3));
        compareIterators(list.listIterator(5), reference.listIterator(5));
        compareListIterators(list.listIterator(), reference.listIterator());
        compareListIterators(list.listIterator(2), reference.listIterator(2));
        compareListIterators(list.listIterator(3), reference.listIterator(3));
        compareListIterators(list.listIterator(5), reference.listIterator(5));
    }

    private void compareIterators(Iterator<Object> it1, Iterator<Object> it2) {
        while (it1.hasNext()) {
            assertEquals(it1.next(), it2.next());
        }
        assertFalse(it2.hasNext());
    }

    private void compareListIterators(ListIterator<Object> it1, ListIterator<Object> it2) {
        while (it1.hasPrevious()) {
            assertEquals(it1.previous(), it2.previous());
        }
        assertFalse(it2.hasPrevious());
        compareIterators(it1, it2);
    }

    @Test
    public void sublist() {
        assertTrue(Arrays.equals(list.subList(0, 5).toArray(), reference.toArray()));
        assertTrue(Arrays.equals(list.subList(2, 4).toArray(), reference.subList(2, 4).toArray()));
        assertTrue(list.subList(0, 0).isEmpty());
        assertTrue(list.subList(5, 5).isEmpty());
    }

    @Test
    public void sublistMod() {

        List<Object> sl = reference.subList(2, 4);
        reference.remove(0);
        try {
            sl.get(0);
            fail("Exception expected");
        } catch (ConcurrentModificationException cme) {

        }
        sl = list.subList(2, 4);
        listPop();
        assertEquals(4, list.size());
        try {
            sl.get(0);
            fail("Exception expected");
        } catch (ConcurrentModificationException cme) {

        }
    }

    @Test
    public void iteratorMod() {

        ListIterator<Object> iter = reference.listIterator();
        reference.remove(0);
        iter.previousIndex();
        iter.nextIndex();
        iter.hasNext();
        try {
            iter.next();
            fail("Exception expected");
        } catch (ConcurrentModificationException cme) {

        }
        iter = list.listIterator();
        listPop();
        assertEquals(4, list.size());
        iter.previousIndex();
        iter.nextIndex();
        iter.hasNext();
        try {
            iter.next();
            fail("Exception expected");
        } catch (ConcurrentModificationException cme) {

        }
    }

    private void listPop() {
        try (Context context = Context.enter()) {
            ScriptableObject scope = context.initStandardObjects();
            scope.put("list", scope, list);
            context.evaluateString(scope, "list.pop()", "testsrc", 1, null);
        }
    }

    @Test
    public void bigList() {
        try (Context context = Context.enter()) {
            ScriptableObject scope = context.initStandardObjects();
            NativeArray array =
                    (NativeArray)
                            context.evaluateString(
                                    scope, "new Array(4294967295)", "testsrc", 1, null);
            assertEquals(4294967295L, array.getLength());
            try {
                array.size();
                fail("Exception expected");
            } catch (IllegalStateException e) {
                assertEquals("list.length (4294967295) exceeds Integer.MAX_VALUE", e.getMessage());
            }
        }
    }
}
