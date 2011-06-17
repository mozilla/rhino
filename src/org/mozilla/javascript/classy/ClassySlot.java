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
import java.lang.reflect.*;
import org.mozilla.javascript.classy.ClassyLayout.*;
import org.mozilla.javascript.ScriptRuntime;
import static org.mozilla.javascript.classy.ClassyLayout.EMPTY_SLOT_VALUE;

/**
 * A single element of a ClassyLayout; corresponds to a physical or logical variable of some sort.
 * <p>
 *
 * @see org.mozilla.javascript.classy.ClassyScriptable
 * @author John R. Rose
 */

public abstract class ClassySlot implements Slot {
    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /// Methods of interface Slot:
    /** What is the name of this slot?  It is either a String or a SpecialName. */
    public          Object name() { return name; }
    /** What is the type of this slot? */
    public          Object type() { return type; }
    /** What is the unique index of this slot in a compact ordering in this layout? */
    public abstract int logicalIndex();
    /** Method for getting this slot.  For fast paths: Assumes extension slot is already allocated. */
    public abstract MethodHandle getter();
    /** Method for setting this slot.  For fast paths: Assumes extension slot is already allocated. */
    public abstract MethodHandle setter();
    /** Method for getting this slot.  Returns null if this slot does not exist. */
    public          MethodHandle safeGetter() { return getter(); }
    /** Method for setting this slot, which expands the physical layout as necessary. */
    public          MethodHandle safeSetter() { return setter(); }
    /** Is this slot a physical slot, i.e., a Java field with a fixed name? */
    public abstract boolean isPhysical();
    /** Is this slot an extension slot, i.e., a hidden resizable array of values? */
    public          boolean isExtension() { return false; }
    /** Is this non-physical slot allocated as a direct slot?  If not, it is allocated within an extension array. */
    public          boolean isDirect() { return false; }
    /** If this slot is not direct, what is the slot that backs it up? */
    public          Slot physicalSlot() { assert(!isPhysical()); return null; }
    /** If this slot is not direct, what is the index within an extension array? */
    public          int physicalIndex() { assert(!isPhysical()); return 0; }

    final Object name;
    final Object type;

    ClassySlot(Object name, Object type) {
        this.name = name;
        this.type = type;
    }

    boolean assertMethodHandleTypesOK() {
        boolean asserting = false;
        assert (asserting = true);
        if (!asserting)  return true;
        MethodType gtype = getter().type();
        MethodType stype = setter().type();
        assert gtype.parameterCount() == 1;
        assert stype.parameterCount() == 2;
        Class<?> vtype = gtype.returnType();
        Class<?> btype = gtype.parameterType(0);
        assert stype.parameterType(0) == btype;
        assert stype.parameterType(1) == vtype;
        assert stype.returnType() == void.class;
        assert safeGetter().type() == gtype;
        assert safeSetter().type() == stype;
        return true;
    }

    public static class PhysicalSlot extends ClassySlot {
        final int          index;
        final MethodHandle getter;   // get this slot from an object
        final MethodHandle setter;   // set this slot (known to exist) in an object
        public PhysicalSlot(Object name,
                            int index,
                            MethodHandle getter,
                            MethodHandle setter) {
            super(name, getter.type().returnType());
            this.index  = index;
            this.getter = getter;
            this.setter = setter;
            assert assertMethodHandleTypesOK();
        }
        @Override public int    logicalIndex() { return index; }
        @Override public MethodHandle getter() { return getter; }
        @Override public MethodHandle setter() { return setter; }
        @Override public boolean  isPhysical() { return true; }
        @Override public boolean    isDirect() { return true; }
    }

    public static class PhysicalExtensionSlot extends PhysicalSlot {
        public PhysicalExtensionSlot(Object name,
                                     int index,
                                     MethodHandle getter,
                                     MethodHandle setter) {
            super(name, index, getter, setter);
            assert ((Class)type()).isArray();
        }
        @Override public boolean isExtension() { return true; }
    }

    static abstract class LogicalSlot extends ClassySlot {
        final Slot physicalSlot;
        @Override public boolean isPhysical() { return false; }
        @Override public Slot physicalSlot() { return physicalSlot; }
        @Override public int physicalIndex() { return -1; }
        public LogicalSlot(Object name,
                           Object type,
                           Slot physicalSlot) {
            super(name, type);
            assert type == physicalSlot.type();
            this.physicalSlot = physicalSlot;
        }
    }

    static class DirectSlot extends LogicalSlot {
        @Override public int logicalIndex() { return physicalSlot.logicalIndex(); }
        @Override public boolean isDirect() { return true; }
        @Override public MethodHandle getter() { return physicalSlot.getter(); }
        @Override public MethodHandle setter() { return physicalSlot.setter(); }
        public DirectSlot(Object name,
                          Object type,
                          Slot physicalSlot) {
            super(name, type, physicalSlot);
            assert !physicalSlot.isExtension();
        }
    }

    static class ExtendedSlot extends LogicalSlot {
        final int physicalIndex;
        final MethodHandle getter;
        final MethodHandle setter;
        final MethodHandle safeGetter;
        final MethodHandle safeSetter;
        @Override public int logicalIndex() { return physicalSlot.logicalIndex() + physicalIndex; }
        @Override public int physicalIndex() { return physicalIndex; }
        @Override public MethodHandle getter() { return getter; }
        @Override public MethodHandle setter() { return setter; }
        @Override public MethodHandle safeGetter() { return safeGetter; }
        @Override public MethodHandle safeSetter() { return safeSetter; }
        public ExtendedSlot(Object name,
                            Object type,
                            Slot physicalSlotArg,
                            final int physicalIndexArg) {
            super(name, type, physicalSlotArg);
            assert physicalSlot.isExtension();
            this.physicalIndex = physicalIndexArg;
            class ExtendedSlotHandle extends SlotHandle {
                @Override public Object get(Object base) throws Throwable {
                    return getArray(base)[physicalIndex];
                }
                @Override public void set(Object base, Object value) throws Throwable {
                    getArray(base)[physicalIndex] = value;
                }
                @Override public Object safeGet(Object base) throws Throwable {
                    return ensureArray(base)[physicalIndex];
                }
                @Override public void safeSet(Object base, Object value) throws Throwable {
                    ensureArray(base)[physicalIndex] = value;
                }

                public ExtendedSlotHandle(boolean isSet, boolean isSafe) {
                    super(isSet, isSafe);
                }

                final MethodHandle physicalGetter = physicalSlot.getter();  // hoist
                Object[] getArray(Object base) throws Throwable {
                    return (Object[])physicalGetter.invoke(base);
                }
                Object[] ensureArray(Object base) throws Throwable {
                    MethodHandle getter = physicalSlot.safeGetter();
                    Object[] a = (Object[])getter.invoke(base);
                    if (a == null) {
                        a = new Object[initialArraySize(physicalIndex, OBJECT_GRAIN)];
                    } else if (physicalIndex >= a.length) {
                        int nextlen = nextArraySize(physicalIndex, a.length, OBJECT_GRAIN);
                        //System.out.println("for "+base+" reallocating from "+a.length+" to "+nextlen);
                        a = Arrays.copyOf(a, nextlen);
                    } else {
                        return a;
                    }
                    MethodHandle setter = physicalSlot.safeSetter();
                    setter.invoke(base, (Object[]) a);
                    return a;
                }
            }
            this.getter = new ExtendedSlotHandle(false, false);
            this.setter = new ExtendedSlotHandle(true,  false);
            MethodHandle safeGetter = getter;
            MethodHandle safeSetter = setter;
            if (mightReallocateArray(physicalIndex, OBJECT_GRAIN)) {
                safeGetter = new ExtendedSlotHandle(false, true);
                safeSetter = new ExtendedSlotHandle(true, true);
            }
            this.safeGetter = safeGetter;
            this.safeSetter = safeSetter;
        }

        static final int OBJECT_GRAIN = 8;  // allocate at least 8 slots at a time (maybe more)
    }

    static boolean mightReallocateArray(int index, int grain) {
        return (index & (grain-1)) == 0;
    }
    static int initialArraySize(int index, int grain) {
        return (index + grain) & ~(grain-1);
    }
    static int nextArraySize(int index, int size, int grain) {
        if ((index | size | (index + size)) < 0)
            return Integer.MAX_VALUE;  // don't go negative
        int extra = (size >> 2);  // 25% overhead buys logarithmic number of resizes
        return initialArraySize(index + extra, grain);
    }

    static class ArrayIndexedSlot extends LogicalSlot implements ClassyLayout.IndexedSlot {
        final LogicalSlot arraySlot;
        final MethodHandle safeIndexedGetter;
        final MethodHandle safeIndexedSetter;
        @Override public int logicalIndex() { return arraySlot.logicalIndex(); }
        @Override public int physicalIndex() { return arraySlot.physicalIndex(); }
        @Override public MethodHandle getter() { return arraySlot.getter(); }
        @Override public MethodHandle setter() { return arraySlot.setter(); }
        @Override public MethodHandle safeGetter() { return arraySlot.safeGetter(); }
        @Override public MethodHandle safeSetter() { return arraySlot.safeSetter(); }
        @Override public MethodHandle safeIndexedGetter() { return safeIndexedGetter; }
        @Override public MethodHandle safeIndexedSetter() { return safeIndexedSetter; }

        public ArrayIndexedSlot(LogicalSlot arraySlot) {
            super(arraySlot.name(), arraySlot.type(), arraySlot.physicalSlot());
            this.arraySlot = arraySlot;
            this.safeIndexedGetter = MethodHandles.insertArguments(MH_safeIndexedGet, 0, this);
            this.safeIndexedSetter = MethodHandles.insertArguments(MH_safeIndexedSet, 0, this);
        }

        Object safeIndexedGet(Object base, Object index) throws Throwable {
            if (index instanceof Integer ||
                (index = ScriptRuntime.toStringIdOrIndex(index)) instanceof Integer) {
                return fastGet(base, (int)(Integer)index);
            } else {
                // Slow path...
                return ScriptRuntime.getObjectElem(base, index);
            }
        }
        private static final MethodHandle MH_safeIndexedGet =
            findVirtual(ArrayIndexedSlot.class, "safeIndexedGet",
                        MethodType.methodType(Object.class, Object.class, Object.class));

        void safeIndexedSet(Object base, Object index, Object value) throws Throwable {
            if (index instanceof Integer ||
                (index = ScriptRuntime.toStringIdOrIndex(index)) instanceof Integer) {
                fastSet(base, (Integer)index, value);
            } else {
                // Slow path...
                ScriptRuntime.setObjectElem(base, index, value);
            }
        }
        private static final MethodHandle MH_safeIndexedSet =
            findVirtual(ArrayIndexedSlot.class, "safeIndexedSet",
                        MethodType.methodType(void.class, Object.class, Object.class, Object.class));

        private Object getArray(Object base) throws Throwable {
            return ((MethodHandle)arraySlot.getter()).invoke(base);
        }
        private void setArray(Object base, Object array) throws Throwable {
            ((MethodHandle)arraySlot.setter()).invoke(base, array);
        }

        private Object fastGet(Object base, Integer index) throws Throwable {
            Object array = getArray(base);
            if (array instanceof Object[]) {
                Object[] a = (Object[]) array;
                int pos = (int)(Integer)a[0] + (int)index;
                if (pos > 0 && pos < a.length) {
                    return a[pos];
                }
                return EMPTY_SLOT_VALUE;
            } else {
                return slowGet(array, base, index);
            }
        }
        private Object slowGet(Object array, Object base, Integer index) throws Throwable {
            if (array instanceof Map) {
                Map<Integer,Object> map = (Map<Integer,Object>) array;
                Object value = map.get(index);
                //FIXME: distinguish undefined from null
                return value;
            } else {
                return EMPTY_SLOT_VALUE;
            }
        }
        private void fastSet(Object base, Integer index, Object value) throws Throwable {
            Object array = getArray(base);
            if (array instanceof Object[]) {
                Object[] a = (Object[]) array;
                int pos = (int)(Integer)a[0] + (int)index;
                if (pos > 0 && pos < a.length) {
                    a[pos] = value;
                    return;
                }
            }
            slowSet(array, base, index, value);
        }

        public static final Object[] EMPTY_ARRAY = {1};

        private void slowSet(Object array, Object base, Integer index, Object value) throws Throwable {
            if (array == null)
                array = EMPTY_ARRAY;  // bootstrap from empty initial state
            if (array instanceof Object[]) {
                Object[] a = (Object[]) array;
                int off = (int)(Integer)a[0], length = a.length;
                int pos = off + (int)index;
                // FIXME:  Keep track of density in the array.
                int nextlen = Integer.MAX_VALUE, nextoff = off, limit = (length/4 + ARRAY_GRAIN);
                if (pos >= length && (pos - length) <= limit) {
                    nextlen = nextArraySize(pos + 1, length, ARRAY_GRAIN);
                } else if (pos <= 0 && pos >= -limit) {
                    nextlen = nextArraySize(length + pos+1, length, ARRAY_GRAIN);
                    nextoff = 1 - (int)index;
                } else if (length == 1) {
                    nextlen = nextArraySize(1, 0, ARRAY_GRAIN);
                    nextoff = 1 - (int)index;
                }
                if (nextlen < Integer.MAX_VALUE / 4) {
                    Object[] a2 = null;
                    try {
                        a2 = Arrays.copyOf(a, nextlen);
                    } catch (RuntimeException ignore) {
                    }
                    if (a2 != null) {
                        Arrays.fill(a2, length, nextlen, EMPTY_SLOT_VALUE);
                        if (nextoff > off) {
                            // In old array, item at a[1] was for index=(1-off).
                            // In new array, item at a[1] moves to a2[index+nextoff] = a2[1-off+nextoff].
                            System.arraycopy(a2, 1, a2, 1 + (nextoff - off), length-1);
                            Arrays.fill(a2, 1, (nextoff - off), EMPTY_SLOT_VALUE);
                            a2[0] = off = nextoff;
                            pos = off + (int)index;
                        } else {
                            assert(nextoff == off);
                        }
                        a2[pos] = value;
                        setArray(base, a2);
                        return;
                    }
                }
                // Array resizing lost.  Use a sparse map.
                Map<Integer,Object> map = new TreeMap<Integer,Object>();
                for (int i = 0; i < a.length; i++) {
                    Object v = a[i];
                    if (value != EMPTY_SLOT_VALUE)
                        map.put(i, v);
                }
                map.put(index, value);
                setArray(base, map);
                return;
            }
            Map<Integer,Object> map = (Map<Integer,Object>) array;
            map.put(index, value);
        }

        static final int ARRAY_GRAIN = 8;  // allocate at least 8 slots at a time (maybe more)
    }

    public Slot rename(Object name) {
        return new DirectSlot(name, type(), this);
    }
    
    public Slot renameWithIndex(Object name, int index) {
        return new ExtendedSlot(name, type(), this, index);
    }

    private static MethodHandle findVirtual(Class<?> clazz, String name, MethodType type) {
        try {
            return LOOKUP.findVirtual(clazz, name, type);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    // Utility abstract class for building 4-tuples of related method handles.
    static abstract class SlotHandle extends MethodHandle {
        public abstract Object get(Object base) throws Throwable;
        public abstract void set(Object base, Object value) throws Throwable;
        public abstract Object safeGet(Object base) throws Throwable;
        public abstract void safeSet(Object base, Object value) throws Throwable;
        private static final MethodHandle GET
            = findVirtual(SlotHandle.class, "get",
                                 MethodType.methodType(Object.class, Object.class));
        private static final MethodHandle SET
            = findVirtual(SlotHandle.class, "set",
                                 MethodType.methodType(void.class, Object.class, Object.class));
        private static final MethodHandle SAFE_GET
            = findVirtual(SlotHandle.class, "safeGet",
                                 MethodType.methodType(Object.class, Object.class));
        private static final MethodHandle SAFE_SET
            = findVirtual(SlotHandle.class, "safeSet",
                                 MethodType.methodType(void.class, Object.class, Object.class));
        public SlotHandle(boolean isSet, boolean isSafe) {
            super(!isSet ? (!isSafe ? GET : SAFE_GET)
                  :        (!isSafe ? SET : SAFE_SET));
        }
    }

    /** Given a static type proposed as a slot holder,
     *  find all its public non-static non-final fields of type Object or Object[].
     *  These will serve as direct slots and extension slots, respectively.
     *  Return a 2-list of the direct slots and the extension slots.
     */
    static ArrayList<PhysicalSlot> findAllSlots(Class<?> staticType) {
        Field[] fa = staticType.getFields();
        if (fa.length == 0)  return null;
        int ssp = 0;
        ArrayList<PhysicalSlot> slots = new ArrayList<PhysicalSlot>(fa.length);
        final int badMods = (Modifier.FINAL | Modifier.STATIC);
        final int goodMods = (Modifier.PUBLIC);
        boolean haveExt = false;
        Class<?> fclassPrev = null;
        for (Field f : fa) {
            String name = f.getName();
            Class<?> type = f.getType();
            int mods = f.getModifiers();
            Class<?> fclass = f.getDeclaringClass();
            if (fclassPrev != fclass) {
                if (fclassPrev != null && fclass.isAssignableFrom(fclassPrev))
                    ssp = 0;  // enumerate superclass fields at the front
                fclassPrev = fclass;
            }
            if ((mods & badMods) != 0 || (mods & goodMods) != goodMods)
                continue;
            MethodHandle getter, setter;
            try {
                getter = PUBLIC_LOOKUP.unreflectGetter(f);
                setter = PUBLIC_LOOKUP.unreflectSetter(f);
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
            getter = MethodHandles.convertArguments(getter, MethodType.methodType(type, Object.class));
            setter = MethodHandles.convertArguments(setter, MethodType.methodType(void.class, Object.class, type));
            if (type == Object.class) {
                slots.add(ssp++, new PhysicalSlot(name, 0, getter, setter));
            } else if (type == Object[].class && !haveExt) {
                // There can only be one, and it must be last, or else the logicalIndex numbering won't work.
                slots.add(new PhysicalExtensionSlot(name, 0, getter, setter));
                haveExt = true;
            } else {
                throw new RuntimeException("bad field: "+f);
            }
        }
        // Renumber.
        for (int index = 0; index < slots.size(); index++) {
            PhysicalSlot slot = slots.get(index);
            PhysicalSlot slot2;
            if (slot instanceof PhysicalExtensionSlot) {
                slot2 = new PhysicalExtensionSlot(slot.name(), index, slot.getter(), slot.setter());
            } else {
                slot2 = new PhysicalSlot(slot.name(), index, slot.getter(), slot.setter());
            }
            assert(slot.getClass() == slot2.getClass());
            slots.set(index, slot2);
        }
        return slots;
    }

    static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();

    /**
     * The root of a layout is a set of unassigned physical slots.
     */
    static class PhysicalSlots {
        final Class<?> staticType;
        final List<PhysicalSlot> directSlots;
        final List<PhysicalExtensionSlot> extensionSlots;
        PhysicalSlots(Class<?> staticType) {
            this(staticType, findAllSlots(staticType));
        }
        PhysicalSlots(Class<?> staticType, ArrayList<PhysicalSlot> allSlots) {
            this.staticType = staticType;
            ArrayList<PhysicalExtensionSlot> extSlots = new ArrayList<PhysicalExtensionSlot>(1);
            for (PhysicalSlot slot : allSlots) {
                if (slot instanceof PhysicalExtensionSlot) {
                    extSlots.add((PhysicalExtensionSlot) slot);
                }
            }
            allSlots.removeAll(extSlots);
            this.directSlots    = Collections.unmodifiableList(Arrays.asList(allSlots.toArray(new PhysicalSlot[0])));
            this.extensionSlots = Collections.unmodifiableList(Arrays.asList(extSlots.toArray(new PhysicalExtensionSlot[0])));;
        }
        public String name() {
            return staticType.getSimpleName();
        }
        public String toString() {
            return name() + directSlots + extensionSlots;
        }
    }

    public String toString() {
        String s = name.toString(); // + ":" + logicalIndex();
        if (!isPhysical()) {
            s += "/" + physicalSlot();
            if (physicalSlot().isExtension()) {
                s += "[" + physicalIndex() + "]";
            }
        }
        return s;
    }
}
