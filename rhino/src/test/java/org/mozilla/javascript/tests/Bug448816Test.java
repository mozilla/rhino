/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=448816
 *
 * @author Hannes Wallnoefer
 */
public class Bug448816Test {

    Map<Object, Object> map, reference;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        // set up a reference map
        reference = new LinkedHashMap<Object, Object>();
        reference.put("a", "a");
        reference.put("b", Boolean.TRUE);
        reference.put("c", new HashMap<Object, Object>());
        reference.put(Integer.valueOf(1), Integer.valueOf(42));
        // get a js object as map
        try (Context context = Context.enter()) {
            ScriptableObject scope = context.initStandardObjects();
            map =
                    (Map<Object, Object>)
                            context.evaluateString(
                                    scope,
                                    "({ a: 'a', b: true, c: new java.util.HashMap(), 1: 42});",
                                    "testsrc",
                                    1,
                                    null);
        }
    }

    @Test
    public void testEqual() {
        // FIXME we do not override equals() and hashCode() in ScriptableObject
        // so calling this with swapped argument fails. This breaks symmetry
        // of equals(), but overriding these methods might be risky.
        assertEquals(reference, map);
    }

    @Test
    public void basicAccess() {
        assertTrue(map.size() == 4);
        assertEquals(map.get("a"), reference.get("a"));
        assertEquals(map.get("b"), reference.get("b"));
        assertEquals(map.get("c"), reference.get("c"));
        assertEquals(map.get(Integer.valueOf(1)), reference.get(Integer.valueOf(1)));
        assertEquals(map.get("notfound"), reference.get("notfound"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsValue(Boolean.TRUE));
        assertFalse(map.containsKey("x"));
        assertFalse(map.containsValue(Boolean.FALSE));
        assertFalse(map.containsValue(null));
    }

    @Test
    public void collections() {
        assertEquals(map.keySet(), reference.keySet());
        assertEquals(map.entrySet(), reference.entrySet());
        // java.util.Collection does not imply overriding equals(), so:
        assertTrue(map.values().containsAll(reference.values()));
        assertTrue(reference.values().containsAll(map.values()));
    }

    @Test
    public void removal() {
        // the only update we implement is removal
        assertTrue(map.size() == 4);
        assertEquals(map.remove("b"), Boolean.TRUE);
        reference.remove("b");
        assertTrue(map.size() == 3);
        assertEquals(reference, map);
        collections();
    }

    @Test
    public void keyIterator() {
        compareIterators(map.keySet().iterator(), reference.keySet().iterator());
    }

    @Test
    public void entryIterator() {
        compareIterators(map.entrySet().iterator(), reference.entrySet().iterator());
    }

    @Test
    public void valueIterator() {
        compareIterators(map.values().iterator(), reference.values().iterator());
    }

    private void compareIterators(Iterator<?> it1, Iterator<?> it2) {
        assertTrue(map.size() == 4);
        while (it1.hasNext()) {
            assertEquals(it1.next(), it2.next());
            it1.remove();
            it2.remove();
        }
        assertTrue(map.isEmpty());
    }
}
