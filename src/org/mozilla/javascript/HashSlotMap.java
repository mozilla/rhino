/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * This class implements the SlotMap interface using a java.util.HashMap. This class has more
 * overhead than EmbeddedSlotMap, especially because it puts each "Slot" inside an intermediate
 * object. However it is much more resistant to large number of hash collisions than EmbeddedSlotMap
 * and therefore we use this implementation when an object gains a large number of properties.
 */
public class HashSlotMap implements SlotMap {

    private final LinkedHashMap<Object, Slot> map = new LinkedHashMap<>();

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Slot query(Object key, int index) {
        Object name = makeKey(key, index);
        return map.get(name);
    }

    @Override
    public Slot modify(Object key, int index, int attributes) {
        Object name = makeKey(key, index);
        Slot slot = map.get(name);
        if (slot != null) {
            return slot;
        }

        return createSlot(key, index, attributes);
    }

    @Override
    public void replace(Slot oldSlot, Slot newSlot) {
        Object name = makeKey(oldSlot);
        map.put(name, newSlot);
    }

    private Slot createSlot(Object key, int index, int attributes) {
        Slot newSlot = new Slot(key, index, attributes);
        add(newSlot);
        return newSlot;
    }

    @Override
    public void add(Slot newSlot) {
        Object name = makeKey(newSlot);
        map.put(name, newSlot);
    }

    @Override
    public void remove(Object key, int index) {
        Object name = makeKey(key, index);
        Slot slot = map.get(name);
        if (slot != null) {
            // non-configurable
            if ((slot.getAttributes() & ScriptableObject.PERMANENT) != 0) {
                Context cx = Context.getContext();
                if (cx.isStrictMode()) {
                    throw ScriptRuntime.typeErrorById(
                            "msg.delete.property.with.configurable.false", key);
                }
                return;
            }
            map.remove(name);
        }
    }

    @Override
    public Iterator<Slot> iterator() {
        return map.values().iterator();
    }

    private Object makeKey(Object name, int index) {
        return name == null ? String.valueOf(index) : name;
    }

    private Object makeKey(Slot slot) {
        return slot.name == null ? String.valueOf(slot.indexOrHash) : slot.name;
    }
}
