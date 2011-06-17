/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   John R. Rose, Sun Microsystems
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

// API class

package org.mozilla.javascript.classy;

import java.util.*;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import static org.mozilla.javascript.classy.ClassyLayout.*;

/**
 * Alternate implementation of Scriptable which uses shared class-like
 * descriptors to manage object layouts which are compressed.
 * The layouts are not frozen, however, since an object can be given
 * a new classy descriptor.
 *
 * @see org.mozilla.javascript.Scriptable
 * @see org.mozilla.javascript.ScriptableObject
 * @author John Rose
 */

public abstract class ClassyScriptable extends ScriptableObject
        implements Scriptable, HasLayout {
    public static final boolean ENABLED;  // to turn this on, run rhino with -J-Dorg.mozilla.javascript.classy=true
    static {
        boolean ENABLED_ = false;
        try {
            ENABLED_ = Boolean.getBoolean("org.mozilla.javascript.classy");
            if (ENABLED_)  System.out.println("recognized option -Dorg.mozilla.javascript.classy=true");
        } finally {
        }
        ENABLED = ENABLED_;
    }

    static final ClassyLayout ROOT_LAYOUT = makeRootLayout();

    public ClassyScriptable() {
        this.layout = ROOT_LAYOUT;
    }

    public ClassyScriptable(ClassyLayout layout) {
        this.layout = layout;
    }

    public static class Slot {
        final Object name;
        Object value;
        int attributes;

        public Slot(Object name) {
            this.name = name;
        }

        /**
         * The slot's name.
         * @return either a String or a SpecialName.
         */
        public Object name() {
            return name;
        }

        public Object getValue(Scriptable start) {
            return value;
        }

        public void setValue(Object value, Scriptable owner, Scriptable start) {
            this.value = value;
        }

        /**
         * Get the attributes of the slot.
         * @return the attributes
         */
        public int getAttributes() {
            return attributes;
        }

        /**
         * Set the attributes of the slot.
         * @param attributes the attributes
         */
        public void setAttributes(int attributes) {
            this.attributes = attributes;
        }

    }

    protected ClassyLayout layout;
    private Slot[] slots;

    protected Slot extendLayoutWith(Object name) {
        ClassyLayout newLayout = layout.safeExtendWith(name);
        Mapping mapping = newLayout.lastMapping();
        assert(mapping.name().equals(name));
        Slot slot = new Slot(name);
        if (slots == null) {
            slots = new Slot[4];
        } else if (layout.length() >= slots.length) {
            Slot[] newSlots = new Slot[slots.length + 8];
            System.arraycopy(slots, 0, newSlots, 0, slots.length);
            slots = newSlots;
        }
        slots[layout.length()] = slot;
        layout = newLayout;
        return slot;
    }

    public ClassyLayout getLayout() {
        return layout;
    }

    public void setLayout(ClassyLayout layout) {
        this.layout = layout;
    }

    static final boolean USE_INDEXED_PROPERTIES = false;

    /** Placeholder names.  All other names are interned Java strings or java Integers. */
    public enum SpecialName {
        /** The key for the internal Java array that manages all indexed properties. */
        INDEXED_PROPERTIES,
        /** The key for an internal Java map that manages properties which don't fit into the layout. */
        HASHED_PROPERTIES,
        /** The key for fetching [[Class]]. */
        CLASS_NAME,
        /** The key for fetching parent scope, whatever that is. */
        PARENT_SCOPE,
    }

    private Slot findSlot(Object name) {
        Mapping mapping = layout.findMapping(name);
        return mapping == null ? null : slots[mapping.offset()];
    }

    /*
     * Implements one step of the ECMA [[Get]] operator.
     * The field string must already be known <em>not</em> to be an {@code int} spelling.
     * This is the slow path; use partial evaluation & invokedynamic for speed.
     */
    public Object get(String name, Scriptable start) {
        Object rawValue = rawGetImpl(name, start);
        return translateNullToNotFound(rawValue);
    }

    protected Object rawGetImpl(Object name, Scriptable start) {
        // Note the straight-through branchless calling.  That makes it optimizable!
        Slot slot = findSlot(name);
        if (slot == null)
            return EMPTY_SLOT_VALUE;
        return slot.getValue(start);
    }

    public Object get(int index, Scriptable start) {
        Object rawValue = rawGetImpl(Integer.valueOf(index), start);
        return translateNullToNotFound(rawValue);
    }

    /*
     * Implements one step of the ECMA [[HasProperty]] operator.
     * This is the slow path; use partial evaluation & invokedynamic for speed.
     */
    public boolean has(String name, Scriptable start) {
        return hasImpl(name, start);
    }

    protected boolean hasImpl(Object name, Scriptable start) {
        return layout.findMapping(name) != null;
    }

    public boolean has(int index, Scriptable start) {
        return hasImpl(Integer.valueOf(index), start);
    }

    /*
     * Implements the ECMA [[Put]] operator.
     * This is the slow path; use partial evaluation & invokedynamic for speed.
     */
    public void put(String name, Scriptable start, Object value) {
        //int foo = ++putctr; if ((foo & (foo - 1)) == 0) { System.out.println("*** put #"+foo+" "+name+" in "+start+" layout="+(start instanceof HasLayout ? ((HasLayout)start).getLayout() : null)); Thread.dumpStack(); }
        Object rawValue = translateNullToWrapper(value);
        rawPutImpl(name, start, rawValue);
    }

    protected void rawPutImpl(Object name, Scriptable start, Object value) {
        Slot slot = findSlot(name);
        if (slot == null)
            slot = extendLayoutWith(name);
        slot.setValue(value, this, start);
    }

    public void put(int index, Scriptable start, Object value) {
        rawPutImpl(Integer.valueOf(index), start, value);
    }

    /**
     * Implements the ECMA [[Delete]] except that no result is returned.
     */
    public void delete(String name) {
        deleteImpl(name);
    }

    public void deleteImpl(Object name) {
        Slot slot = findSlot(name);
        if (slot == null)
            return;
        // FIXME implement deletion
    }

    public void delete(int index) {
        deleteImpl(Integer.valueOf(index));
    }

    public Scriptable getPrototype() {
        return (Scriptable) layout.prototype();
    }

    public void setPrototype(Scriptable prototype) {
        layout = layout.changePrototype(prototype);
    }

    public Scriptable getParentScope() {
        // We don't use this, since classy scriptables are not functions.
        return (Scriptable) rawGetImpl(SpecialName.PARENT_SCOPE, this);
    }

    public void setParentScope(Scriptable parentScope) {
        rawPutImpl(SpecialName.PARENT_SCOPE, this, parentScope);
    }

    // Other randomn ECMA operators.

    /**
     * Implements the ECMA [[Class]] operator.
     */
    public String getClassName() {
        String cname = (String) rawGetImpl(SpecialName.CLASS_NAME, this);
        if (cname == EMPTY_SLOT_VALUE)
            cname = "Object";  // default if slot is missing
        return cname;
    }

    public void setClassName(String cname) {
        rawPutImpl(SpecialName.CLASS_NAME, this, cname);
    }

    /**
     * Implements the EMCA [[DefaultValue]] operator.
     */
    public Object getDefaultValue(Class<?> hint) {
        return ScriptableObject.getDefaultValue(this, hint);
    }

    /**
     * Implements the EMCA [[HasInstance]] operator.
     */
    public boolean hasInstance(Scriptable instance) {
        return ScriptRuntime.jsDelegatesTo(instance, this);
    }

    /**
     * Get an array of property ids.
     */
    // TO DO: Figure out if this must be defensively copied, or if it can be shared.
    public Object[] getIds() {
        List<Mapping> mappings = layout.mappings();
        ArrayList<Object> ids = new ArrayList<Object>(mappings.size());
        for (Mapping mapping : mappings) {
            Object name = mapping.name();
            if (name instanceof SpecialName) {
                switch ((SpecialName) name) {
                case INDEXED_PROPERTIES:
                    assert(false); // NYI
                    break;
                case HASHED_PROPERTIES:
                    assert(false); //NYI
                    break;
                }
                continue;
            }
            try {
                Object rawValue = slots[mapping.offset()];
                if (rawValue == EMPTY_SLOT_VALUE)  continue;
            } catch (Throwable ex) {
                throw uncheckedException(ex);
            }
            assert(name instanceof String || name instanceof Integer);
            ids.add(name);
        }
        return ids.toArray();
    }

    static ClassyLayout makeRootLayout() {
        return new ClassyLayout(null);
    }

}
