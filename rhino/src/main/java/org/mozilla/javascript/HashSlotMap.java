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

    private final LinkedHashMap<Object, Slot> map;

    public HashSlotMap() {
        map = new LinkedHashMap<>();
    }

    protected HashSlotMap(int capacity) {
        map = new LinkedHashMap<>(capacity);
    }

    public HashSlotMap(SlotMap oldMap) {
        map = new LinkedHashMap<>(oldMap.size());
        for (Slot n : oldMap) {
            add(null, n.copySlot());
        }
    }

    public HashSlotMap(SlotMap oldMap, Slot newSlot) {
        map = new LinkedHashMap<>(oldMap.dirtySize() + 1);
        for (Slot n : oldMap) {
            add(null, n.copySlot());
        }
        add(null, newSlot);
    }

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
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        Object name = makeKey(key, index);
        return map.computeIfAbsent(name, n -> new Slot(key, index, attributes));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
        Object name = makeKey(key, index);
        Slot ret = map.compute(name, (n, existing) -> c.compute(key, index, existing));
        return (S) ret;
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        Object name = makeKey(newSlot);
        map.put(name, newSlot);
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
