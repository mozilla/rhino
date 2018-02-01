/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;
import java.util.LinkedHashMap;
import static org.mozilla.javascript.ScriptableObject.SlotAccess.*;

/**
 * This class implements the SlotMap interface using a java.util.HashMap. This class has more
 * overhead than EmbeddedSlotMap, especially because it puts each "Slot" inside an intermediate
 * object. However it is much more resistant to large number of hash collisions than
 * EmbeddedSlotMap and therefore we use this implementation when an object gains a large
 * number of properties.
 */

public class HashSlotMap
    implements SlotMap {

    private final LinkedHashMap<Object, ScriptableObject.Slot> map =
        new LinkedHashMap<Object, ScriptableObject.Slot>();

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public ScriptableObject.Slot query(Object key, int index)
    {
        Object name = key == null ? String.valueOf(index) : key;
        return map.get(name);
    }

    @Override
    public ScriptableObject.Slot get(Object key, int index, ScriptableObject.SlotAccess accessType) {
        Object name = key == null ? String.valueOf(index) : key;
        ScriptableObject.Slot slot = map.get(name);
        switch (accessType) {
            case QUERY:
                return slot;
            case MODIFY:
            case MODIFY_CONST:
                if (slot != null)
                    return slot;
                break;
            case MODIFY_GETTER_SETTER:
                if (slot instanceof ScriptableObject.GetterSlot)
                    return slot;
                break;
            case CONVERT_ACCESSOR_TO_DATA:
                if ( !(slot instanceof ScriptableObject.GetterSlot) )
                    return slot;
                break;
        }

        return createSlot(key, index, name, accessType);
    }

    private ScriptableObject.Slot createSlot(Object key, int index,
        Object name, ScriptableObject.SlotAccess accessType) {
        ScriptableObject.Slot slot = map.get(name);
        if (slot != null) {
            ScriptableObject.Slot newSlot;

            if (accessType == MODIFY_GETTER_SETTER
                    && !(slot instanceof ScriptableObject.GetterSlot)) {
                newSlot = new ScriptableObject.GetterSlot(name, slot.indexOrHash, slot.getAttributes());
            } else if (accessType == CONVERT_ACCESSOR_TO_DATA
                    && (slot instanceof ScriptableObject.GetterSlot)) {
                newSlot = new ScriptableObject.Slot(name, slot.indexOrHash, slot.getAttributes());
            } else if (accessType == MODIFY_CONST) {
                return null;
            } else {
                return slot;
            }
            newSlot.value = slot.value;
            map.put(name, newSlot);
            return newSlot;
        }

        ScriptableObject.Slot newSlot = (accessType == MODIFY_GETTER_SETTER
                ? new ScriptableObject.GetterSlot(key, index, 0)
                : new ScriptableObject.Slot(key, index, 0));
        if (accessType == MODIFY_CONST) {
            newSlot.setAttributes(ScriptableObject.CONST);
        }
        addSlot(newSlot);
        return newSlot;
    }

    @Override
    public void addSlot(ScriptableObject.Slot newSlot) {
        Object name = newSlot.name == null ? String.valueOf(newSlot.indexOrHash) : newSlot.name;
        map.put(name, newSlot);
    }

    @Override
    public void remove(Object key, int index) {
        Object name = key == null ? String.valueOf(index) : key;
        ScriptableObject.Slot slot = map.get(name);
        if (slot != null) {
            // non-configurable
            if ((slot.getAttributes() & ScriptableObject.PERMANENT) != 0) {
                Context cx = Context.getContext();
                if (cx.isStrictMode()) {
                    throw ScriptRuntime.typeError1("msg.delete.property.with.configurable.false", key);
                }
                return;
            }
            map.remove(name);
        }
    }

    @Override
    public Iterator<ScriptableObject.Slot> iterator() {
        return map.values().iterator();
    }
}
