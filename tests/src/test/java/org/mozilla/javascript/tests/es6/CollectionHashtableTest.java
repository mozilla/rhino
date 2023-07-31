/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Hashtable;
import org.mozilla.javascript.Hashtable.Entry;
import org.mozilla.javascript.Undefined;

/**
 * These are some tests for the Hashtable implementation that's used by the collection classes in
 * Rhino.
 */
public class CollectionHashtableTest {
    private Hashtable ht;

    @Before
    public void init() {
        ht = new Hashtable();
    }

    /** Sanity test on empty table. */
    @Test
    public void empty() {
        assertEquals(0, ht.size());
        assertNull(null, ht.getEntry("one"));
        assertFalse(ht.has("one"));
        assertNull(ht.delete("one"));
        assertFalse(ht.deleteEntry("one"));
        ht.clear();
        assertEquals(0, ht.size());
        for (Hashtable.Entry e : ht) {
            fail("Should be no entries in the hash table");
        }
    }

    /** Just a sanity test for the basic functionality. */
    @Test
    public void crud() {
        ht.put("one", 1);
        assertEquals(1, ht.size());
        assertEquals(1, ht.getEntry("one").value());
        assertTrue(ht.has("one"));
        ht.put("two", 2);
        assertEquals(2, ht.size());
        assertEquals(2, ht.getEntry("two").value());
        assertTrue(ht.has("two"));
        ht.clear();
        assertEquals(0, ht.size());
        assertNull(ht.getEntry("one"));
        assertFalse(ht.has("one"));
    }

    /** Basic iteration test */
    @Test
    public void basicIteration() {
        ht.put("one", 1);
        ht.put("two", 2);

        Iterator<Entry> i = ht.iterator();
        assertTrue(i.hasNext());
        Hashtable.Entry e = i.next();
        assertEquals("one", e.key());
        assertEquals(1, e.value());

        assertTrue(i.hasNext());
        e = i.next();
        assertEquals("two", e.key());
        assertEquals(2, e.value());

        assertFalse(i.hasNext());
    }

    /** Test that elements appear in insertion order. */
    @Test
    public void insertionOrder() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());
        e = i.next();
        assertEquals("e", e.key());
        e = i.next();
        assertEquals("b", e.key());
        assertFalse(i.hasNext());
    }

    /** Test that they appear in insertion order even if they are modified. */
    @Test
    public void insertionOrderModified() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);
        ht.put("e", 4);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());
        e = i.next();
        assertEquals("e", e.key());
        e = i.next();
        assertEquals("b", e.key());
        assertFalse(i.hasNext());
    }

    /** Test that they appear in insertion order when deleted and recreated. */
    @Test
    public void insertionOrderDeleted() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);
        assertTrue(ht.deleteEntry("e"));
        ht.put("e", 5);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());
        e = i.next();
        assertEquals("b", e.key());
        e = i.next();
        assertEquals("e", e.key());
        assertFalse(i.hasNext());
    }

    /**
     * Test that they appear in insertion order when deleted and recreated. This versions used the
     * deprecated delete method instead of deleteEntry
     */
    @Test
    public void insertionOrderDeletedDeprecated() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);
        assertNotNull(ht.delete("e"));
        ht.put("e", 5);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());
        e = i.next();
        assertEquals("b", e.key());
        e = i.next();
        assertEquals("e", e.key());
        assertFalse(i.hasNext());
    }

    /** Test that we can add elements while an iterator exists and see the newer elements. */
    @Test
    public void insertionOrderAfterIterating() {
        ht.put("a", 1);
        ht.put("e", 2);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());
        e = i.next();
        assertEquals("e", e.key());

        ht.put("b", 3);
        e = i.next();
        assertEquals("b", e.key());
        assertFalse(i.hasNext());
    }

    /** Test "clear" operation and iteration. */
    @Test
    public void insertionOrderAfterClear() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());
        e = i.next();
        assertEquals("e", e.key());
        e = i.next();
        assertEquals("b", e.key());

        ht.clear();
        ht.put("b", 4);

        e = i.next();
        assertEquals("b", e.key());
        assertFalse(i.hasNext());
    }

    /** Test that we can delete elements while iterating. */
    @Test
    public void insertionOrderWithDelete() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());

        assertTrue(ht.deleteEntry("e"));

        e = i.next();
        assertEquals("b", e.key());
        assertFalse(i.hasNext());
    }

    /** Test that we can delete elements while iterating. */
    @Test
    public void insertionOrderWithDeleteDeprecated() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("a", e.key());

        assertNotNull(ht.delete("e"));

        e = i.next();
        assertEquals("b", e.key());
        assertFalse(i.hasNext());
    }

    /**
     * Worst-case scenario -- delete everything the "hard way" and re-create while iterator exists.
     */
    @Test
    public void deleteAllWhileIterating() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);

        Iterator<Entry> i = ht.iterator();

        assertTrue(ht.deleteEntry("a"));
        assertTrue(ht.deleteEntry("e"));
        assertTrue(ht.deleteEntry("b"));

        ht.put("aa", 10);
        ht.put("ee", 20);
        ht.put("bb", 30);

        Hashtable.Entry e = i.next();
        assertEquals("aa", e.key());
        e = i.next();
        assertEquals("ee", e.key());
        e = i.next();
        assertEquals("bb", e.key());
        assertFalse(i.hasNext());
    }

    /**
     * Worst-case scenario -- delete everything the "hard way" and re-create while iterator exists.
     */
    @Test
    public void deleteAllWhileIteratingDeprecated() {
        ht.put("a", 1);
        ht.put("e", 2);
        ht.put("b", 3);

        Iterator<Entry> i = ht.iterator();

        assertNotNull(ht.delete("a"));
        assertNotNull(ht.delete("e"));
        assertNotNull(ht.delete("b"));

        ht.put("aa", 10);
        ht.put("ee", 20);
        ht.put("bb", 30);

        Hashtable.Entry e = i.next();
        assertEquals("aa", e.key());
        e = i.next();
        assertEquals("ee", e.key());
        e = i.next();
        assertEquals("bb", e.key());
        assertFalse(i.hasNext());
    }

    /** Test serialization of an empty object. */
    @Test
    public void emptySerialization() throws IOException, ClassNotFoundException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oout = new ObjectOutputStream(bos)) {
            oout.writeObject(ht);
            oout.close();
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(bis);
        Hashtable sht = (Hashtable) oin.readObject();
        assertEquals(0, sht.size());
    }

    /**
     * Test serialization of a non-empty object with a bunch of different data types, including the
     * iteration order.
     */
    @Test
    public void serialization() throws IOException, ClassNotFoundException {

        ht.put("one", 1);
        ht.put("two", 2);
        ht.put("three", 3);
        ht.put(Undefined.instance, "undefined");
        ht.put("undefined", Undefined.instance);
        ht.put(null, "null");
        ht.put("null", null);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oout = new ObjectOutputStream(bos)) {
            oout.writeObject(ht);
            oout.close();
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(bis);
        Hashtable sht = (Hashtable) oin.readObject();

        assertEquals(1, sht.getEntry("one").value());
        assertEquals(2, sht.getEntry("two").value());
        assertEquals(3, sht.getEntry("three").value());
        assertEquals(Undefined.instance, sht.getEntry("undefined").value());
        assertEquals("undefined", sht.getEntry(Undefined.instance).value());
        assertNull(sht.getEntry("null").value());
        assertEquals("null", sht.getEntry(null).value());

        Iterator<Entry> i = ht.iterator();
        Hashtable.Entry e = i.next();
        assertEquals("one", e.key());
        e = i.next();
        assertEquals("two", e.key());
        e = i.next();
        assertEquals("three", e.key());
        e = i.next();
        assertEquals(Undefined.instance, e.key());
        e = i.next();
        assertEquals("undefined", e.key());
        e = i.next();
        assertNull(e.key());
        e = i.next();
        assertEquals("null", e.key());
        assertFalse(i.hasNext());
    }
}
