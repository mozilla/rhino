/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Hashtable;
import org.mozilla.javascript.Hashtable.Entry;

/**
 * These are some tests for the Hashtable implementation that's used by the collection
 * classes in Rhino.
 */
public class CollectionHashtableTest
{
  private Hashtable ht;

  @Before
  public void init() {
    ht = new Hashtable();
  }

  /**
   * Sanity test on empty table.
   */
  @Test
  public void testEmpty() {
    assertEquals(0, ht.size());
    assertNull(null, ht.get("one"));
    assertFalse(ht.has("one"));
    assertNull(ht.delete("one"));
    ht.clear();
    assertEquals(0, ht.size());
    for (Hashtable.Entry e : ht) {
      fail("Should be no entries in the hash table");
    }
  }

  /**
   * Just a sanity test for the basic functionality.
   */
  @Test
  public void testCRUD() {
    ht.put("one", 1);
    assertEquals(1, ht.size());
    assertEquals(1, ht.get("one"));
    assertTrue(ht.has("one"));
    ht.put("two", 2);
    assertEquals(2, ht.size());
    assertEquals(2, ht.get("two"));
    assertTrue(ht.has("two"));
    ht.clear();
    assertEquals(0, ht.size());
    assertNull(ht.get("one"));
    assertFalse(ht.has("one"));
  }


  /**
   * Basic iteration test
   */
  @Test
  public void testBasicIteration() {
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

  /**
   * Test that elements appear in insertion order.
   */
  @Test
  public void testInsertionOrder() {
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

  /**
   * Test that they appear in insertion order even if they are modified.
   */
  @Test
  public void testInsertionOrderModified() {
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

  /**
   * Test that they appear in insertion order when deleted and recreated.
   */
  @Test
  public void testInsertionOrderDeleted() {
    ht.put("a", 1);
    ht.put("e", 2);
    ht.put("b", 3);
    ht.delete("e");
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
   * Test that we can add elements while an iterator exists and see the newer elements.
   */
  @Test
  public void testInsertionOrderAfterIterating() {
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

  /**
   * Test "clear" operation and iteration.
   */
  @Test
  public void testInsertionOrderAfterClear() {
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

  /**
   * Test that we can delete elements while iterating.
   */
  @Test
  public void testInsertionOrderWithDelete() {
    ht.put("a", 1);
    ht.put("e", 2);
    ht.put("b", 3);

    Iterator<Entry> i = ht.iterator();
    Hashtable.Entry e = i.next();
    assertEquals("a", e.key());

    ht.delete("e");

    e = i.next();
    assertEquals("b", e.key());
    assertFalse(i.hasNext());
  }

  /**
   * Worst-case scenario -- delete everything the "hard way" and re-create while iterator
   * exists.
   */
  @Test
  public void testDeleteAllWhileIterating() {
    ht.put("a", 1);
    ht.put("e", 2);
    ht.put("b", 3);

    Iterator<Entry> i = ht.iterator();

    ht.delete("a");
    ht.delete("e");
    ht.delete("b");

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
}
