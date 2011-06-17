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
import java.lang.invoke.*;
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

public abstract class ClassyScriptable implements Scriptable, HasLayout {
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

    protected ClassyLayout layout;

    public ClassyScriptable(ClassyLayout layout) {
        this.layout = layout;
    }

    // These are the mappable fields, picked up in the NULL_LAYOUT:
    public static class Struct4 extends ClassyScriptable {
        public final static ClassyLayout NULL_LAYOUT = makeRootLayout(Struct4.class);
        public Struct4(ClassyLayout layout) { super(layout); }
        public Object f0, f1, f2, f3;
        public Object[] fext;
    }
    public static class Struct8 extends Struct4 {
        public final static ClassyLayout NULL_LAYOUT = makeRootLayout(Struct8.class);
        public Struct8(ClassyLayout layout) { super(layout); }
        public Object f4, f5, f6, f7;
    }

    // For objects (like method-rich prototypes) known to be larger, maybe add Struct20, even Struct100.

    protected Slot extendLayoutWith(Object name) {
        ClassyLayout layout2 = layout.safeExtendWith(name);
        Slot slot = layout2.lastSlot();
        assert(slot.name().equals(name));
        this.layout = layout2;
        return slot;
    }
    public ClassyLayout getLayout() { return layout; }
    public void setLayout(ClassyLayout newlo) { layout = newlo; }

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

    /*
     * Implements one step of the ECMA [[Get]] operator.
     * The field string must already be known <em>not</em> to be an {@code int} spelling.
     * This is the slow path; use partial evaluation & invokedynamic for speed.
     */
    @Override
    public Object get(String name, Scriptable start) {
        //int foo = ++getctr; if ((foo & (foo - 1)) == 0) { System.out.println("*** get #"+foo+" "+name+" in "+start+" layout="+(start instanceof HasLayout ? ((HasLayout)start).getLayout() : null)); Thread.dumpStack(); }
        Object rawValue = rawGetImpl(name, start);
        return translateNullToNotFound(rawValue);
    }
    static int getctr;

    protected Object rawGetImpl(Object name, Scriptable start) {
        // Note the straight-through branchless calling.  That makes it optimizable!
        Slot slot = layout.findSlot(name);
        // In particular, the part up to the final MH.invoke can be memoized in an
        // invokedynamic instruction, as longs an isApplicablePartial guard is also present.
        // FIXME:  What if the field has a JS getter function, and that function
        // wants to play continuation or generator games?
        if (slot == null)  return EMPTY_SLOT_VALUE;
        try {
            MethodHandle getter = slot.safeGetter();
            return getter.invoke((Object)this);
        } catch (Throwable ex) {
            throw uncheckedException(ex);
        }
    }

    @Override
    public Object get(int index, Scriptable start) {
        Object rawValue = EMPTY_SLOT_VALUE;
        if (USE_INDEXED_PROPERTIES) {
            IndexedSlot slot = (IndexedSlot) layout.findSlot(SpecialName.INDEXED_PROPERTIES);
            if (slot != null) {
                try {
                    MethodHandle getter = slot.safeIndexedGetter();
                    rawValue = getter.invoke((Object)this, index);
                } catch (Throwable ex) {
                    throw uncheckedException(ex);
                }
            }
        }
        if (rawValue == EMPTY_SLOT_VALUE)
            rawValue = rawGetImpl(index, start);
        return translateNullToNotFound(rawValue);
    }

    /*
     * Implements one step of the ECMA [[HasProperty]] operator.
     * This is the slow path; use partial evaluation & invokedynamic for speed.
     */
    @Override
    public boolean has(String name, Scriptable start) {
        return hasImpl(name, start);
    }

    protected boolean hasImpl(Object name, Scriptable start) {
        Slot slot = layout.findSlot(name);
        if (slot == null)  return false;
        try {
            MethodHandle getter = slot.safeGetter();
            return (getter.invoke((Object)this) != EMPTY_SLOT_VALUE);
        } catch (Throwable ex) {
            throw uncheckedException(ex);
        }
    }

    @Override
    public boolean has(int index, Scriptable start) {
        if (USE_INDEXED_PROPERTIES) {
            IndexedSlot slot = (IndexedSlot) layout.findSlot(SpecialName.INDEXED_PROPERTIES);
            if (slot != null) {
                try {
                    MethodHandle getter = slot.safeIndexedGetter();
                    Object rawValue = getter.invoke((Object)this, index);
                    if (rawValue != EMPTY_SLOT_VALUE)
                        return true;
                } catch (Throwable ex) {
                    throw uncheckedException(ex);
                }
            }
        }
        return hasImpl(index, start);
    }

    /*
     * Implements the ECMA [[Put]] operator.
     * This is the slow path; use partial evaluation & invokedynamic for speed.
     */
    @Override
    public void put(String name, Scriptable start, Object value) {
        //int foo = ++putctr; if ((foo & (foo - 1)) == 0) { System.out.println("*** put #"+foo+" "+name+" in "+start+" layout="+(start instanceof HasLayout ? ((HasLayout)start).getLayout() : null)); Thread.dumpStack(); }
        Object rawValue = translateNullToWrapper(value);
        rawPutImpl(name, start, rawValue);
    }
    static int putctr;

    protected void rawPutImpl(Object name, Scriptable start, Object rawValue) {
        // Cf. ScriptableObject.putImpl which also handles logic for sealed/READONLY, GetterSlot, and this!=start
        Slot slot = layout.findSlot(name);
        if (slot == null)  slot = extendLayoutWith(name);
        try {
            MethodHandle setter = slot.safeSetter();
            setter.invoke(this, rawValue);
        } catch (Throwable ex) {
            throw uncheckedException(ex);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        Object rawValue = translateNullToWrapper(value);
        if (USE_INDEXED_PROPERTIES) {
            IndexedSlot slot = (IndexedSlot) layout.findSlot(SpecialName.INDEXED_PROPERTIES);
            assert(slot != null);  // FIXME
            try {
                MethodHandle setter = slot.safeIndexedSetter();
                setter.invoke(this, index, rawValue);
                return;
            } catch (Throwable ex) {
                throw uncheckedException(ex);
            }
        }
        rawPutImpl(index, start, rawValue);
    }

    /**
     * Implements the ECMA [[Delete]] except that no result is returned.
     */
    @Override
    public void delete(String name) {
        deleteImpl(name);
    }

    public void deleteImpl(Object name) {
        Slot slot = layout.findSlot(name);
        if (slot == null)  return;
        // This doesn't quite work.  The layout should be marked as containing holes at this point.
        // Or we should just revert to a "one size fits all" fallback layout.
        try {
            MethodHandle setter = slot.safeSetter();
            setter.invoke((Object)this, EMPTY_SLOT_VALUE);
        } catch (Throwable ex) {
            throw uncheckedException(ex);
        }
    }

    @Override
    public void delete(int index) {
        if (USE_INDEXED_PROPERTIES) {
            IndexedSlot slot = (IndexedSlot) layout.findSlot(SpecialName.INDEXED_PROPERTIES);
            if (slot != null) {
                try {
                    MethodHandle setter = slot.safeIndexedSetter();
                    setter.invoke((Object)this, index, EMPTY_SLOT_VALUE);
                } catch (Throwable ex) {
                    throw uncheckedException(ex);
                }
            }
        }
        deleteImpl(index);
    }

    // Overloads of [[Get]] with special property tokens.

    @Override
    public Scriptable getPrototype() {
        return (Scriptable) layout.prototype();
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        layout = layout.changePrototype(prototype);
    }

    @Override
    public Scriptable getParentScope() {
        // We don't use this, since classy scriptables are not functions.
        return (Scriptable) rawGetImpl(SpecialName.PARENT_SCOPE, this);
    }

    @Override
    public void setParentScope(Scriptable parentScope) {
        rawPutImpl(SpecialName.PARENT_SCOPE, this, parentScope);
    }

    // Other randomn ECMA operators.

    /**
     * Implements the ECMA [[Class]] operator.
     */
    @Override
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
    @Override
    public Object getDefaultValue(Class<?> hint) {
        return ScriptableObject.getDefaultValue(this, hint);
    }

    /**
     * Implements the EMCA [[HasInstance]] operator.
     */
    @Override
    public boolean hasInstance(Scriptable instance) {
        return ScriptRuntime.jsDelegatesTo(instance, this);
    }

    /**
     * Get an array of property ids.
     */
    // TO DO: Figure out if this must be defensively copied, or if it can be shared.
    @Override
    public Object[] getIds() {
        List<Slot> slots = layout.slots();
        ArrayList<Object> ids = new ArrayList<Object>(slots.size());
        for (Slot slot : slots) {
            Object name = slot.name();
            if (name instanceof SpecialName) {
                switch ((SpecialName) name) {
                case INDEXED_PROPERTIES:
                    assert(USE_INDEXED_PROPERTIES && slot instanceof IndexedSlot);
                    // add properties of this slot to the mix
                    try {
                        Object props = ((MethodHandle)slot.getter()).invoke((Object)this);
                        assert(false); // NYI
                    } catch (Throwable ex) {
                        throw uncheckedException(ex);
                    }
                    break;
                case HASHED_PROPERTIES:
                    assert(false); //NYI
                    break;
                }
                continue;
            }
            try {
                MethodHandle getter = slot.safeGetter();
                Object rawValue = getter.invoke((Object)this);
                if (rawValue == EMPTY_SLOT_VALUE)  continue;
            } catch (Throwable ex) {
                throw uncheckedException(ex);
            }
            assert(name instanceof String || name instanceof Integer);
            ids.add(name);
        }
        return ids.toArray();
    }

    static ClassyLayout makeRootLayout(Class<?> staticType) {
        if (Boolean.getBoolean("regular-getters") || !Struct4.class.isAssignableFrom(staticType)) {
            System.out.println("[Note] Using slow regular slot setters for "+staticType);
            return ClassyLayout.makeRootLayout(staticType);
        } else {
            // FIXME: Get rid of all this special stuff.  (Optimize MethodHandles.Lookup.unreflectGetter, etc.)
            MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
            MethodType getType = MethodType.methodType(Object.class, Object.class);
            MethodType setType = MethodType.methodType(void.class, Object.class, Object.class);
            ArrayList<ClassySlot.PhysicalSlot> allSlots = new ArrayList<ClassySlot.PhysicalSlot>();
            int index = 0;
            try {
                for (String name : new String[] { "f0", "f1", "f2", "f3", "f4", "f5", "f6", "f7" }) {
                    if (name.equals("f4") && !Struct8.class.isAssignableFrom(staticType))
                        break;
                    allSlots.add(new ClassySlot.PhysicalSlot(name, index++,
                            LOOKUP.findStatic(ClassyScriptable.class, name, getType),
                            LOOKUP.findStatic(ClassyScriptable.class, name, setType)));
                }
                getType = getType.changeReturnType(Object[].class);
                setType = setType.changeParameterType(1, Object[].class);
                allSlots.add(new ClassySlot.PhysicalExtensionSlot("fext", index++,
                        LOOKUP.findStatic(ClassyScriptable.class, "fext", getType),
                        LOOKUP.findStatic(ClassyScriptable.class, "fext", setType)));
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
            return ClassyLayout.makeRootLayout(staticType, allSlots);
        }
    }
    static Object f0(Object receiver) { return ((Struct4)receiver).f0; }
    static void f0(Object receiver, Object value) { ((Struct4)receiver).f0 = value; }
    static Object f1(Object receiver) { return ((Struct4)receiver).f1; }
    static void f1(Object receiver, Object value) { ((Struct4)receiver).f1 = value; }
    static Object f2(Object receiver) { return ((Struct4)receiver).f2; }
    static void f2(Object receiver, Object value) { ((Struct4)receiver).f2 = value; }
    static Object f3(Object receiver) { return ((Struct4)receiver).f3; }
    static void f3(Object receiver, Object value) { ((Struct4)receiver).f3 = value; }
    static Object[] fext(Object receiver) { return ((Struct4)receiver).fext; }
    static void fext(Object receiver, Object[] value) { ((Struct4)receiver).fext = value; }
    static Object f4(Object receiver) { return ((Struct8)receiver).f4; }
    static void f4(Object receiver, Object value) { ((Struct8)receiver).f4 = value; }
    static Object f5(Object receiver) { return ((Struct8)receiver).f5; }
    static void f5(Object receiver, Object value) { ((Struct8)receiver).f5 = value; }
    static Object f6(Object receiver) { return ((Struct8)receiver).f6; }
    static void f6(Object receiver, Object value) { ((Struct8)receiver).f6 = value; }
    static Object f7(Object receiver) { return ((Struct8)receiver).f7; }
    static void f7(Object receiver, Object value) { ((Struct8)receiver).f7 = value; }
}
