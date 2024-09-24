/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This generic hash table class is used by Set and Map. It uses a standard HashMap for storing keys
 * and values so that we can handle lots of hash collisions if necessary, and a doubly-linked list
 * to support the iterator capability.
 *
 * <p>This second one is important because JavaScript handling of the iterator is completely
 * different from the way that Java does it. In Java an attempt to modify a collection on a HashMap
 * or LinkedHashMap while iterating through it (except by using the "remove" method on the Iterator
 * object itself) results in a ConcurrentModificationException. JavaScript Maps and Sets explicitly
 * allow the collection to be modified, or even cleared completely, while iterators exist, and even
 * lets an iterator keep on iterating on a collection that was empty when it was created..
 */
@SuppressWarnings("ReferenceEquality")
public class Hashtable implements Serializable, Iterable<Hashtable.Entry> {

    private static final long serialVersionUID = -7151554912419543747L;
    private final HashMap<Object, Entry> map = new HashMap<>();
    private Entry first = null;
    private Entry last = null;

    /**
     * One entry in the hash table. Override equals and hashcode because this is another area in
     * which JavaScript and Java differ. This entry also becomes a node in the linked list.
     */
    public static final class Entry implements Serializable {
        private static final long serialVersionUID = 4086572107122965503L;
        Object key;
        Object value;
        boolean deleted;
        Entry next;
        Entry prev;
        final int hashCode;

        Entry() {
            hashCode = 0;
        }

        Entry(Object k, Object value) {
            if (k instanceof Number) {
                if (k instanceof Double || k instanceof BigInteger) {
                    // BigInteger needs to retain its own type, due to
                    // "If Type(x) is different from Type(y), return false." in
                    // ecma262/multipage/abstract-operations.html#sec-samevaluezero
                    key = k;
                } else {
                    // Hash comparison won't work if we don't do this
                    key = Double.valueOf(((Number) k).doubleValue());
                }
            } else if (k instanceof ConsString) {
                key = k.toString();
            } else {
                key = k;
            }

            if (key == null) {
                hashCode = 0;
            } else if (k.equals(ScriptRuntime.negativeZeroObj)) {
                hashCode = 0;
            } else {
                hashCode = key.hashCode();
            }

            this.value = value;
        }

        public Object key() {
            return key;
        }

        public Object value() {
            return value;
        }

        /** Zero out key and value and return old value. */
        void clear() {
            key = Undefined.instance;
            value = Undefined.instance;
            deleted = true;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            try {
                return ScriptRuntime.sameZero(key, ((Entry) o).key);
            } catch (ClassCastException cce) {
                return false;
            }
        }
    }

    private static Entry makeDummy() {
        final Entry d = new Entry();
        d.clear();
        return d;
    }

    public int size() {
        return map.size();
    }

    public void put(Object key, Object value) {
        final Entry nv = new Entry(key, value);

        map.compute(
                nv,
                (k, existing) -> {
                    if (existing == null) {
                        // New value -- insert to end of doubly-linked list
                        if (first == null) {
                            first = last = nv;
                        } else {
                            last.next = nv;
                            nv.prev = last;
                            last = nv;
                        }
                        return nv;
                    } else {
                        // Update the existing value and keep it in the same place in the list
                        existing.value = value;
                        return existing;
                    }
                });
    }

    /**
     * @deprecated use getEntry(Object key) instead because this returns null if the entry was not
     *     found or the value of the entry is null
     */
    @Deprecated
    public Object get(Object key) {
        final Entry e = new Entry(key, null);
        final Entry v = map.get(e);
        if (v == null) {
            return null;
        }
        return v.value;
    }

    public Entry getEntry(Object key) {
        final Entry e = new Entry(key, null);
        return map.get(e);
    }

    public boolean has(Object key) {
        final Entry e = new Entry(key, null);
        return map.containsKey(e);
    }

    /**
     * @deprecated use deleteEntry(Object key) instead because this returns null if the entry was
     *     not found or the value of the entry is null
     */
    @Deprecated
    public Object delete(Object key) {
        final Entry e = new Entry(key, null);
        final Entry v = map.remove(e);
        if (v == null) {
            return null;
        }

        // To keep existing iterators moving forward as specified in EC262,
        // we will remove the "prev" pointers from the list but leave the "next"
        // pointers intact. Once we do that, then the only things pointing to
        // the deleted nodes are existing iterators. Once those are gone, then
        // these objects will be GCed.
        // This way, new iterators will not "see" the deleted elements, and
        // existing iterators will continue from wherever they left off to
        // continue iterating in insertion order.
        if (v == first) {
            if (v == last) {
                // Removing the only element. Leave it as a dummy or existing iterators
                // will never stop.
                v.clear();
                v.prev = null;
            } else {
                first = v.next;
                first.prev = null;
                if (first.next != null) {
                    first.next.prev = first;
                }
            }
        } else {
            final Entry prev = v.prev;
            prev.next = v.next;
            v.prev = null;
            if (v.next != null) {
                v.next.prev = prev;
            } else {
                assert (v == last);
                last = prev;
            }
        }
        // Still clear the node in case it is in the chain of some iterator
        final Object ret = v.value;
        v.clear();
        return ret;
    }

    public boolean deleteEntry(Object key) {
        final Entry e = new Entry(key, null);
        final Entry v = map.remove(e);
        if (v == null) {
            return false;
        }

        // To keep existing iterators moving forward as specified in EC262,
        // we will remove the "prev" pointers from the list but leave the "next"
        // pointers intact. Once we do that, then the only things pointing to
        // the deleted nodes are existing iterators. Once those are gone, then
        // these objects will be GCed.
        // This way, new iterators will not "see" the deleted elements, and
        // existing iterators will continue from wherever they left off to
        // continue iterating in insertion order.
        if (v == first) {
            if (v == last) {
                // Removing the only element. Leave it as a dummy or existing iterators
                // will never stop.
                v.clear();
                v.prev = null;
            } else {
                first = v.next;
                first.prev = null;
                if (first.next != null) {
                    first.next.prev = first;
                }
            }
        } else {
            final Entry prev = v.prev;
            prev.next = v.next;
            v.prev = null;
            if (v.next != null) {
                v.next.prev = prev;
            } else {
                assert (v == last);
                last = prev;
            }
        }
        // Still clear the node in case it is in the chain of some iterator
        v.clear();
        return true;
    }

    public void clear() {
        // Zero out all the entries so that existing iterators will skip them all
        Iterator<Entry> it = iterator();
        it.forEachRemaining(Entry::clear);

        // Replace the existing list with a dummy, and make it the last node
        // of the current list. If new nodes are added now, existing iterators
        // will drive forward right into the new list. If they are not, then
        // nothing is referencing the old list and it'll get GCed.
        if (first != null) {
            Entry dummy = makeDummy();
            last.next = dummy;
            first = last = dummy;
        }

        // Now we can clear the actual hashtable!
        map.clear();
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iter(first);
    }

    // The iterator for this class works directly on the linked list so that it implements
    // the specified iteration behavior, which is very different from Java.
    private static final class Iter implements Iterator<Entry> {
        private Entry pos;

        Iter(Entry start) {
            // Keep the logic simpler by having a dummy at the start
            Entry dummy = makeDummy();
            dummy.next = start;
            this.pos = dummy;
        }

        private void skipDeleted() {
            // Skip forward past deleted elements, which could appear due to
            // "delete" or a "clear" operation after this iterator was created.
            // End up just before the next non-deleted node.
            while ((pos.next != null) && pos.next.deleted) {
                pos = pos.next;
            }
        }

        @Override
        public boolean hasNext() {
            skipDeleted();
            return ((pos != null) && (pos.next != null));
        }

        @Override
        public Entry next() {
            skipDeleted();
            if ((pos == null) || (pos.next == null)) {
                throw new NoSuchElementException();
            }
            final Entry e = pos.next;
            pos = pos.next;
            return e;
        }
    }
}
