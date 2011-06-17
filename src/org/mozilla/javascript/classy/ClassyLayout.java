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

/**
 * Sharable struct-like descriptor for specifying a compressed object layout.
 * <p>
 *
 * @see org.mozilla.javascript.classy.ClassyScriptable
 * @author John R. Rose
 */

public class ClassyLayout {
    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public interface HasLayout {
        ClassyLayout getLayout();
        void setLayout(ClassyLayout layout);
    }

    /*
    public static final Object WRAPPED_NULL_VALUE = org.mozilla.javascript.UniqueTag.NULL_VALUE;
    public static final Object EMPTY_SLOT_SIGNAL  = org.mozilla.javascript.UniqueTag.NOT_FOUND;
    public static final Object EMPTY_SLOT_VALUE   = null;
    */
    public static final Object WRAPPED_NULL_VALUE = null;
    public static final Object EMPTY_SLOT_SIGNAL  = org.mozilla.javascript.UniqueTag.NOT_FOUND;
    public static final Object EMPTY_SLOT_VALUE   = org.mozilla.javascript.UniqueTag.NOT_FOUND;

    /**
     * A Slot can hold one value.
     */
    // This is the API only.  The implementation is in ClassySlot.
    public interface Slot {
        /** What is the name of this slot?  It is either a String or a SpecialName. */
        Object name();
        /** What is the type of this slot? */  //FIXME get a type
        Object type();
        /** What is the unique index of this slot in a compact ordering in this layout? */
        int logicalIndex();
        /** Method for getting this slot.  For fast paths: Assumes extension slot is already allocated. */
        MethodHandle getter();
        /** Method for setting this slot.  For fast paths: Assumes extension slot is already allocated. */
        MethodHandle setter();
        /** Method for getting this slot, which returns null if not allocated in the physical layout. */
        MethodHandle safeGetter();
        /** Method for setting this slot, which expands the physical layout as necessary. */
        MethodHandle safeSetter();
        /** Is this slot a physical slot, i.e., a Java field with a fixed name? */
        boolean isPhysical();
        /** Is this slot an extension slot, i.e., a hidden resizable array of values? */
        boolean isExtension();
        /** Is this slot allocated as a direct slot?  If not, it is on an extension array. */
        boolean isDirect();
        /** If this slot is not physical, what is the directs or extension array that backs it up? */
        Slot physicalSlot();
        /** If this slot is not physical and not direct, what is its index within an extension array? */
        int physicalIndex();
    }

    public interface IndexedSlot extends Slot {
        /** Method for getting an element from the underlying array within this slot. */
        MethodHandle safeIndexedGetter();
        /** Method for setting an element from the underlying array within this slot. */
        MethodHandle safeIndexedSetter();
    }

    // FIXME: Rhino probably should use null as the "not found" value,
    // since JVMs are very good at null checks, but not so good at random
    // constant comparisons.  This is a backward compatibility problem, though.

    /** When pulling values out of layouts, translate nulls to "not found" indicators. */
    public static Object translateNullToNotFound(Object x) {
        if (EMPTY_SLOT_VALUE != EMPTY_SLOT_SIGNAL) {
            if (x == EMPTY_SLOT_VALUE)    return EMPTY_SLOT_SIGNAL;
            if (x == WRAPPED_NULL_VALUE)  return null;  // encoded Java null in a live field
        }
        return x;
    }
    /** When pushing values into layouts, translate null indicators to literal null. */
    public static Object translateNullToWrapper(Object x) {
        if (WRAPPED_NULL_VALUE != null)
            if (x == null)  return WRAPPED_NULL_VALUE;  // encoded Java null in a live field
        assert(x != EMPTY_SLOT_SIGNAL);  // use delete, not put
        return x;
    }

    /** Map a dynamically checked exception to a statically checked one. */
    public static RuntimeException uncheckedException(Throwable ex) {
        if (ex instanceof RuntimeException)
            return (RuntimeException) ex;
        if (ex instanceof Error)
            throw (Error) ex;
        InternalError ie = new InternalError("checked exception");
        ie.initCause(ex);
        throw ie;
    }
    public static <T extends Throwable>
    T checkedException(Throwable ex, Class<? extends T> exClass) throws T {
        if (exClass.isInstance(ex))
            return exClass.cast(ex);
        throw uncheckedException(ex);
    }

    private final ClassySlot.PhysicalSlots physicalSlots;
    private final int length;
    private final ClassyLayout prefix;
    private final Slot lastSlot;
    private final Object prototype;
    private Object extensionCache;

    public int length() {
        return length;
    }

    public ClassyLayout prefix() {
        return prefix;
    }

    public Slot lastSlot() {
        return lastSlot;
    }

    public Object prototype() {
        return prototype;
    }

    public boolean equals(Object that) {
        return (that instanceof ClassyLayout && equals((ClassyLayout) that));
    }
    public boolean equals(ClassyLayout that) {
        if (this.length != that.length)  return false;
        if (this.prototype != that.prototype)  return false;
        if (this.physicalSlots != that.physicalSlots)  return false;
        ClassyLayout x = this, y = that;
        while (x != y) {
            if (x.lastSlot.name() != y.lastSlot.name())
                return false;
            x = x.prefix;
            y = y.prefix;
            if (x == null || y == null)  return false;
        }
        return true;
    }
    public int hashCode() {
        int slotCode = 0;
        for (ClassyLayout x = this; x != null; x = x.prefix) {
            slotCode = (slotCode * 31) + x.lastSlot.name().hashCode();
        }
        return slotCode ^ prototype.hashCode() ^ physicalSlots.hashCode();
    }

    public Class<?> staticType() {
        return physicalSlots.staticType;
    }

    private boolean assertFindSlot(Object name, boolean mustHave) {
        assert assertNameNormalized(name);
        Slot found = findSlot(name);
        assert ((found == null) ^ mustHave)
            : (name + (mustHave ? " not found in " : " already in ") + this);

        return true;
    }

    private static final String MAX_NUMERAL = String.valueOf(Integer.MIN_VALUE).substring(1);
    private static final int MAX_NUMERAL_LENGTH = MAX_NUMERAL.length();
    public static Integer integerNumeral(String name) {
        int len = name.length();
        if (len == 0)  return null;
        int d0 = 0;
        char ch = name.charAt(d0);
        if (ch == '-')  ch = name.charAt(++d0);
        if ((ch & -16) != ('0' & -16))  return null;
        if (len > MAX_NUMERAL_LENGTH)   return null;
        if (ch == '0')                  return (d0 == 0 && len == 1) ? Integer.valueOf(0) : null;
        for (int i = d0; i < len; i++) {
            ch = name.charAt(i);
            if (ch < '0' || ch > '9')  return null;
        }
        if (len == MAX_NUMERAL_LENGTH) {
            int cmp = MAX_NUMERAL.compareTo(name);
            if (cmp < 0)              return null;
            if (cmp == 0 && d0 == 0)  return null;
        }
        return Integer.valueOf(name);
    }

    public static Object normalizeName(Object name) {
        if (name instanceof String) {
            String s = (String) name;
            Integer num = integerNumeral(s);
            if (num != null)  return num;
            return s.intern();
        }
        if (name instanceof Integer) {
            return Integer.valueOf((int)(Integer) name);
        }
        return name;
    }
    public static boolean assertNameNormalized(Object name) {
        assert (name == normalizeName(name)) : name;
        return true;
    }

    public ClassyLayout changePrototype(Object newPrototype) {
        if (this.prototype == newPrototype)  return this;
        ClassyLayout copy = new ClassyLayout(physicalSlots, newPrototype);
        for (Slot slot : this.slots()) {
            ClassyLayout nextCopy = new ClassyLayout(copy, slot);
            copy.updateCache(nextCopy);
            copy = nextCopy;
        }
        return copy;
    }

    public ClassyLayout safeExtendWith(Object name) {
        return extendWith(normalizeName(name));
    }
    public ClassyLayout extendWith(Object name) {
        ClassyLayout extension = probeCache(name);
        if (extension != null)  return extension;
        assert assertFindSlot(name, false);
        int staticLength = physicalSlots.directSlots.size();
        Slot nextSlot;
        if (length < staticLength) {
            ClassySlot.PhysicalSlot staticSlot = physicalSlots.directSlots.get(length);
            nextSlot = staticSlot.rename(name);
        } else {
            int extensionIndex = length - staticLength;
            ClassySlot.PhysicalExtensionSlot extensionSlot = null;
            for (ClassySlot.PhysicalExtensionSlot xs : physicalSlots.extensionSlots) {
                if (xs.type == Object[].class) {
                    extensionSlot = xs; break;
                }
            }
            if (extensionSlot == null)
                return null;
            nextSlot = extensionSlot.renameWithIndex(name, extensionIndex);
        }
        if (name == ClassyScriptable.SpecialName.INDEXED_PROPERTIES) {
            nextSlot = new ClassySlot.ArrayIndexedSlot((ClassySlot.LogicalSlot) nextSlot);
            System.out.println("Adding [*] to "+this+" : "+nextSlot);
        }
        extension = new ClassyLayout(this, nextSlot);
        updateCache(extension);
        return extension;
    }
    public ClassyLayout guessExtension() {
        if (!CACHE_LAYOUTS)  return this;
        ClassyLayout x = this;
        for (;;) {
            Object cache = x.extensionCache;
            if (!(cache instanceof ClassyLayout))
                break;
            x = (ClassyLayout) cache;
        }
        return x;
    }
    private ClassyLayout probeCache(Object name) {
        if (!CACHE_LAYOUTS)  return null;
        Object cache = extensionCache;
        if (cache == null) {
            return null;
        }
        if (cache instanceof ClassyLayout) {
            ClassyLayout extension = (ClassyLayout) cache;
            if (extension.lastSlot.name() == name) {
                // Usual case for long slot lists...
                return extension;
            }
        } else {
            ArrayList<ClassyLayout> extensions = asList(cache);
            for (int i = 0, imax = extensions.size(); i < imax; i++) {
                ClassyLayout extension = extensions.get(i);
                if (extension.lastSlot.name() == name) {
                    if (i > 0) {
                        // move to head of cache
                        extensions.set(i, extensions.get(0));
                        extensions.set(0, extension);
                    }
                    return extension;
                }
            }
        }
        return null;
    }
    private void updateCache(ClassyLayout extension) {
        if (!CACHE_LAYOUTS)  return;
        Object cache = extensionCache;
        if (cache == null) {
            extensionCache = extension;
            return;
        }
        ArrayList<ClassyLayout> extensions;
        if (cache instanceof ClassyLayout) {
            extensions = new ArrayList<ClassyLayout>(4);
            extensions.add((ClassyLayout) cache);
        } else {
            extensions = asList(cache);
        }
        extensions.add(extension);
    }
    @SuppressWarnings("unchecked") private static ArrayList<ClassyLayout> asList(Object x) {
        return (ArrayList<ClassyLayout>) x;
    }

    public static ClassyLayout makeRootLayout(Class<?> staticType) {
        Object noProto = null;
        return new ClassyLayout(new ClassySlot.PhysicalSlots(staticType), noProto);
    }

    // FIXME: get rid of this special customization path; unreflectSetter is good enough
    public static ClassyLayout makeRootLayout(Class<?> staticType, ArrayList<ClassySlot.PhysicalSlot> allSlots) {
        Object noProto = null;
        return new ClassyLayout(new ClassySlot.PhysicalSlots(staticType, allSlots), noProto);
    }

    ClassyLayout(ClassySlot.PhysicalSlots physicalSlots, Object prototype) {
        this.physicalSlots = physicalSlots;
        this.length = 0;
        this.prefix = null;
        this.lastSlot = null;
        this.prototype = prototype;
        if (TRACE_LAYOUTS)  System.out.println("New Layout root: "+this);
    }

    private ClassyLayout(ClassyLayout prefix, Slot lastSlot) {
        this.physicalSlots = prefix.physicalSlots;
        this.prototype = prefix.prototype;
        this.length = prefix.length + 1;
        this.prefix = prefix;
        this.lastSlot = lastSlot;
        if (TRACE_LAYOUTS)  System.out.println("New Layout: "+this);
    }

    public Slot safeFindSlot(Object name) {
        for (ClassyLayout p = this; p.lastSlot != null; p = p.prefix) {
            if (p.lastSlot.name().equals(name)) {
                return p.lastSlot;
            }
        }
        return null;
    }

    public Slot findSlot(Object name) {
        for (ClassyLayout p = this; p.lastSlot != null; p = p.prefix) {
            if (p.lastSlot.name() == name) {
                return p.lastSlot;
            }
        }
        assert (safeFindSlot(name) == null);
        return null;
    }

    private static final List<Slot> NO_SLOTS = Collections.unmodifiableList(Arrays.asList(new Slot[0]));
    public List<Slot> slots() {
        if (length == 0)  return NO_SLOTS;
        Slot[] sa = new Slot[length];
        ClassyLayout p = this;
        for (int i = length-1; i >= 0; i--) {
            sa[i] = p.lastSlot;
            p = p.prefix;
        }
        return Collections.unmodifiableList(Arrays.asList(sa));
    }

    public String toString() {
        return physicalSlots.name() + slots().toString() + "/proto=" + prototype + "#" + System.identityHashCode(this);
    }

    // Helper:
    public Map<Slot,Object> describe(Object base) {
        List<Slot> slots = slots();
        Map<Slot,Object> map = new LinkedHashMap<Slot,Object>(slots.size());
        for (Slot slot : slots) {
            Object rawValue;
            try {
                rawValue = slot.safeGetter().invoke(base);
            } catch (Throwable ex) {
                throw uncheckedException(ex);
            }
            Object value = rawValue; //translateNullToNotFound(rawValue);
            map.put(slot, value);
        }
        return map;
    }
    public static Map<Slot,Object> describe(HasLayout base) {
        return base.getLayout().describe((Object) base);
    }

    static final boolean TRACE_LAYOUTS = false;
    static final boolean CACHE_LAYOUTS = true;
}
