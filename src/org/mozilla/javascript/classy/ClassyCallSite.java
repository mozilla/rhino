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

import java.lang.invoke.*;
import java.util.*;
import org.mozilla.javascript.*;
import static org.mozilla.javascript.classy.ClassyLayout.*;

/**
 * Inline caching call sites.
 * <p>
 *
 * @see org.mozilla.javascript.classy.ClassyScriptable
 * @author John R. Rose
 */

public class ClassyCallSite extends MutableCallSite {
    public static final boolean ENABLED;  // to turn this on, run rhino with -J-Dorg.mozilla.javascript.classy.callsite=true
    public static final boolean TRACE_SITES;
    static {
        boolean ENABLED_ = false;
        boolean TRACE_SITES_ = false;
        try {
            ENABLED_ = Boolean.getBoolean("org.mozilla.javascript.classy.callsite");
            if (ENABLED_)  System.out.println("recognized option -Dorg.mozilla.javascript.classy.callsite=true");
            TRACE_SITES_ = Boolean.getBoolean("org.mozilla.javascript.classy.trace.sites");
            if (TRACE_SITES_)  System.out.println("recognized option -Dorg.mozilla.javascript.classy.trace.sites=true");
        } finally {
        }
        ENABLED = ClassyScriptable.ENABLED && ENABLED_;
        TRACE_SITES = TRACE_SITES_;
    }

    private final String referenceKind;
    private final Object propertyName;
    private final MethodHandle slowPath;

    private ClassyCallSite(Object caller, String name, MethodType type) {
        super(type);
        Object[] nameAndKind = computeKindAndName(nameComponents());
        this.referenceKind = (String) nameAndKind[0];
        this.propertyName = nameAndKind[1];
        this.slowPath = computeSlowPath();
        setNeutral();  // start with a neutral pre-monomorphic state
    }

    protected int arity() { return type().parameterCount(); }

    protected boolean isIndexed() { return propertyName == ClassyScriptable.SpecialName.INDEXED_PROPERTIES; }

    static final List<String> ALL_KINDS
        = Arrays.asList("get",  // x.name -or- x[y]
                        "set",  // x.name = v -or- x[y] = v
                        // see Node.DECR_FLAG, Node.POST_FLAG:
                        "incr@0", "incr@1", "incr@2", "incr@3", // ++x.name, --x.name, x++.name, x--.name
                        "call", // x.name(...) -or- x[y](...)
                        "apply", // x.name(*args) -or- x[y](*args)
                        "name",     // (scope.)name
                        "name=",  // (scope.)name = v
                        // FIXME: add delete etc.
                        // FIXME: ? turn into an enum somewhere else
                        null),
        REF_KINDS       = ALL_KINDS.subList(0, ALL_KINDS.indexOf(null)),
        INDEX_REF_KINDS = ALL_KINDS.subList(0, ALL_KINDS.indexOf("apply")+1);  // forms which take x[y]
    static final Character COLON = ':';

    /** Based on the name chosen by the bytecode compiler, decide what is the statically fixed property. */
    static Object[] computeKindAndName(Object[] nc) {
        String kind;
        Object name;
        if (nc.length == 1 && nc[0] instanceof String) {
            // simple <prop> => get:<prop>
            kind = "get";
            name = ClassyLayout.normalizeName(nc[0]);
        } else if (nc.length == 3 && REF_KINDS.contains(nc[0]) &&
                   nc[1] == COLON && nc[2] instanceof String) {
            // get:<prop>, set:<prop>, etc.
            kind = ((String)nc[0]).intern();
            name = ClassyLayout.normalizeName(nc[2]);
        } else if (nc.length == 2 && INDEX_REF_KINDS.contains(nc[0]) &&
                   nc[1] == COLON) {
            // get:, set:, etc., refer to indexed property reference
            kind = ((String)nc[0]).intern();
            name = ClassyScriptable.SpecialName.INDEXED_PROPERTIES;
        } else {
            throw new RuntimeException("unrecognized structured name: "+Arrays.asList(nc));
        }
        return new Object[] { kind, name };
    }

    public static CallSite bootstrap(Class caller, String name, MethodType type) {
        return new ClassyCallSite(caller, name, type);
    }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle MH_slowGet;
    private static final MethodHandle MH_slowSet;
    private static final MethodHandle MH_slowName;
    private static final MethodHandle MH_getNamedFnAndThis;
    private static final MethodHandle MH_slowIncrDecr;
    private static final MethodHandle MH_profileReceiver;
    private static final MethodHandle MH_profileReceiverForMiss;
    // Given a DynamicCallable, invoke it quickly without varargs.
    private static MethodHandle[] MH_callDynamic;
    private static final MethodHandle MH_getFromPrototype_String;
    private static final MethodHandle MH_getFromPrototype_Integer;
    private static final MethodHandle MH_getFromParentScope;
    private static final MethodHandle MH_fastExtendAndSet;
    private static final MethodHandle MH_fastApply_local;
    private static final MethodHandle MH_fastApply_prototype;
    private static final MethodHandle MH_fastIncrDecr;
    private static final MethodHandle MH_checkLayout;
    private static final MethodHandle MH_checkLayout_CS;
    private static final MethodHandle MH_checkPrototype;
    private static final MethodHandle MH_checkPrototype_CS;

    // Random:
    private static final MethodHandle MH_isInstance;


    static {
        try {
            MH_slowGet =
                    LOOKUP.findStatic(ClassyCallSite.class, "slowGet",
                            MethodType.methodType(Object.class, Object.class, Object.class));
            MH_slowSet =
                    LOOKUP.findStatic(ClassyCallSite.class, "slowSet",
                            MethodType.methodType(void.class, Object.class, Object.class, Object.class));
            MH_slowName =
                    LOOKUP.findVirtual(ClassyCallSite.class, "slowName",
                            MethodType.methodType(Object.class, Scriptable.class));
            MH_getNamedFnAndThis =
                    LOOKUP.findStatic(ClassyCallSite.class, "getNamedFnAndThis",
                            MethodType.methodType(CallableAndThis.class, Object.class, Object.class));
            MH_slowIncrDecr =
                    LOOKUP.findVirtual(ClassyCallSite.class, "slowIncrDecr",
                            MethodType.methodType(Object.class, Integer.class, Object.class));
            MH_profileReceiver =
                    LOOKUP.findVirtual(ClassyCallSite.class, "profileReceiver",
                            MethodType.methodType(Object.class, Object.class));
            MH_profileReceiverForMiss =
                    LOOKUP.findVirtual(ClassyCallSite.class, "profileReceiverForMiss",
                            MethodType.methodType(Object.class, Object.class));
            MH_callDynamic = new MethodHandle[] {
                    LOOKUP.findVirtual(DynamicCallable.class, "call0",
                            MethodType.methodType(Object.class, Scriptable.class)),
                    LOOKUP.findVirtual(DynamicCallable.class, "call1",
                            MethodType.methodType(Object.class, Scriptable.class, Object.class)),
                    LOOKUP.findVirtual(DynamicCallable.class, "call2",
                            MethodType.methodType(Object.class, Scriptable.class, Object.class, Object.class)),
                    LOOKUP.findVirtual(DynamicCallable.class, "call3",
                            MethodType.methodType(Object.class, Scriptable.class, Object.class, Object.class, Object.class)),
                    LOOKUP.findVirtual(DynamicCallable.class, "call4",
                            MethodType.methodType(Object.class, Scriptable.class, Object.class, Object.class, Object.class, Object.class)),
            };
            MH_getFromPrototype_String = LOOKUP.
                findStatic(ClassyCallSite.class, "getFromPrototype",
                           MethodType.methodType(Object.class, Scriptable.class, String.class, Object.class));
            MH_getFromPrototype_Integer = LOOKUP.
                findStatic(ClassyCallSite.class, "getFromPrototype",
                           MethodType.methodType(Object.class, Scriptable.class, Integer.class, Object.class));
            MH_getFromParentScope = LOOKUP.
                findStatic(ClassyCallSite.class, "getFromParentScope",
                           MethodType.methodType(Object.class, Scriptable.class, String.class, Scriptable.class));
            MH_fastExtendAndSet = LOOKUP.
                findStatic(ClassyCallSite.class, "fastExtendAndSet",
                           MethodType.methodType(void.class, ClassyLayout.class, MethodHandle.class, Object.class, Object.class));
            MH_fastApply_local = LOOKUP.
                findStatic(ClassyCallSite.class, "fastApply_local",
                           MethodType.methodType(Object.class, Slot.class, Object.class, Object[].class));
            MH_fastApply_prototype = LOOKUP.
                findStatic(ClassyCallSite.class, "fastApply_prototype",
                           MethodType.methodType(Object.class, Callable.class, Object.class, Object[].class));
            MH_fastIncrDecr = LOOKUP.
                findStatic(ClassyCallSite.class, "fastIncrDecr",
                           MethodType.methodType(Object.class, MethodHandle.class, MethodHandle.class, Integer.class, Object.class));
            MH_checkLayout = LOOKUP.
                findStatic(ClassyCallSite.class, "checkLayout",
                           MethodType.methodType(boolean.class, ClassyLayout.class, Object.class));
            MH_checkLayout_CS = LOOKUP.
                findStatic(ClassyCallSite.class, "checkLayout_CS",
                           MethodType.methodType(boolean.class, ClassyLayout.class, Object.class));
            MH_checkPrototype = LOOKUP.
                findStatic(ClassyCallSite.class, "checkPrototype",
                           MethodType.methodType(boolean.class, ClassyLayout.class, Object.class, Object.class, Object.class));
            MH_checkPrototype_CS = LOOKUP.
                findStatic(ClassyCallSite.class, "checkPrototype_CS",
                           MethodType.methodType(boolean.class, ClassyLayout.class, Object.class, Object.class, Object.class));

            // Random:
            MH_isInstance = LOOKUP.
                findVirtual(Class.class, "isInstance", MethodType.methodType(boolean.class, Object.class));

        } catch (Exception x) {
            throw new Error(x);
        }
    }

    protected static Object slowGet(Object receiver, Object propertyName) {
        Object value = ScriptRuntime.getObjectElem(receiver, propertyName);
        return value;
    }

    protected static void slowSet(Object receiver, Object propertyName, Object value) {
        ScriptRuntime.setObjectElem(receiver, propertyName, value);
    }

    protected Object slowName(Scriptable scope) {
        return ScriptRuntime.name(scope, (String) propertyName);
    }

    /*
    static int slowctr=0;
    protected Object slowApply(Object receiver, Object... args) throws Throwable {
        //int foo = ++slowctr; if ((foo & (foo - 1)) == 0) { System.out.println("*** slowApply #"+foo+" "+this+" for "+receiver+" layout="+(receiver instanceof HasLayout ? ((HasLayout)receiver).getLayout() : null)); Thread.dumpStack(); }
        CallableAndThis fnAndThis = ScriptRuntime.getElemFunctionAndThis(receiver, propertyName);
        if (true) // FIXME
            return fnAndThis.fn.call((Context)null, null, fnAndThis.thisObj, args);
        return fnAndThis.callV(args);
    }
    private static final MethodHandle MH_slowApply = LOOKUP.
        findVirtual(ClassyCallSite.class, "slowApply",
                    MethodType.methodType(Object.class, Object.class, Object[].class));
    */

    protected static CallableAndThis getNamedFnAndThis(Object propertyName, Object receiver) {
        return ScriptRuntime.getElemFunctionAndThis(receiver, propertyName);
    }

    protected Object slowIncrDecr(Integer incrDecrMask, Object receiver) {
        Context cx = Context.getCurrentContext();  // yuck, really slow
        return ScriptRuntime.elemIncrDecr(receiver, propertyName, cx, incrDecrMask);
    }

    private static int MONO_TARGET_COUNT = 3;
    private static int POLY_TARGET_COUNT = 30;
    private static int MAX_SPECIALIZED_MISSES = MONO_TARGET_COUNT+2; //2 is too small
    private static int MAX_TOTAL_PROFILES = POLY_TARGET_COUNT*2;

    /** State changing policy.  Invoked from profile points. */
    protected void doPolicy(boolean sawMiss) {
        TypeProfile prof = typeProfile;
        if (prof == null)  return;
        int pc = incrementProfileCount();
        if (pc >= MAX_TOTAL_PROFILES) {
            setMegamorphic();
            return;
        }
        if (sawMiss) {
            // already specialized; delay respecialization
            if (++specializedMisses < MAX_SPECIALIZED_MISSES)
                return;
            specializedMisses >>= 1;
        } else {
            int targetCount = MONO_TARGET_COUNT;
            if (prof.isMonomorphic())
                targetCount = POLY_TARGET_COUNT;
            if (prof.count() <= targetCount)
                return;
        }
        setSpecialized(prof);
    }
    protected Object profileReceiver(Object receiver) {
        return profileReceiver(receiver, false);
    }
    protected Object profileReceiverForMiss(Object receiver) {
        return profileReceiver(receiver, true);
    }
    protected Object profileReceiver(Object receiver, boolean sawMiss) {
        if (receiver instanceof Scriptable);  // force profile point
        TypeProfile prof = typeProfile;
        if (prof == null) {
            typeProfile = prof = makeTypeProfile(receiver);
        } else if (!prof.matchAndIncrement(receiver)) {
            typeProfile = prof = prof.append(makeTypeProfile(receiver));
        }
        /*
        if (TRACE_SITES)  System.out.println("Observation: "+this+ " sees: "+prof);
        */
        doPolicy(sawMiss);
        return receiver;
    }

    protected static Object getFromPrototype(Scriptable prototype, String name, Object receiver) {
        Scriptable start = (Scriptable) receiver;
        Scriptable obj = prototype;
        Object result;
        do {
            result = obj.get(name, start);
            if (result != Scriptable.NOT_FOUND)
                break;
            obj = obj.getPrototype();
        } while (obj != null);
        return result;
    }
    protected static Object getFromPrototype(Scriptable prototype, Integer name, Object receiver) {
        Scriptable start = (Scriptable) receiver;
        Scriptable obj = prototype;
        Object result;
        do {
            result = obj.get(name, start);
            if (result != Scriptable.NOT_FOUND)
                break;
            obj = obj.getPrototype();
        } while (obj != null);
        return result;
    }

    protected static Object getFromParentScope(Scriptable parentScope, String name, Scriptable scope) {
        return ScriptRuntime.name(parentScope, name);
    }

    protected static void fastExtendAndSet(ClassyLayout layout, MethodHandle setter, Object receiver, Object value) throws Throwable {
        ((HasLayout) receiver).setLayout(layout);
        setter.invoke(receiver, value);
    }

    protected static Object fastApply_prototype(Callable fn, Object receiver, Object... args) throws Throwable {
        Scriptable obj = (Scriptable) receiver;
        return fn.call((Context)null, null, obj, args);
        /*
        Context cx = Context.getCurrentContext();  // yuck, really slow
        Scriptable scope = (cx == null ? null : ScriptRuntime.getTopCallScope(cx));
        return fn.call(cx, scope, obj, args);
        */
    }
    protected static Object fastApply_local(Slot slot, Object receiver, Object... args) throws Throwable {
        Object fn = ((MethodHandle)slot.getter()).invoke(receiver);
        Scriptable obj = (Scriptable) receiver;
        if (!(fn instanceof Callable)) {
            CallableAndThis fnAndThis = ScriptRuntime.getElemFunctionAndThis(obj, slot.name());
            fn = fnAndThis.fn;
            assert(obj == fnAndThis.thisObj);
        }
        if (false)
            return ((Callable)fn).call((Context)null, null, obj, args);
        // FIXME:
        Context cx = Context.getCurrentContext();  // yuck, really slow
        Scriptable scope = (cx == null ? null : ScriptRuntime.getTopCallScope(cx));
        return ((Callable)fn).call(cx, scope, obj, args);
    }


    protected static Object fastIncrDecr(MethodHandle getter, MethodHandle setter, Integer incrDecrMask, Object receiver) throws Throwable {
        Object value = getter.invoke(receiver);
        Object update;
        switch (incrDecrMask) {
        case 0:
            value = update = increment(value, 1);
            break;
        case Node.DECR_FLAG:
            value = update = increment(value, -1);
            break;
        case Node.POST_FLAG:
            update = increment(value, 1);
            break;
        default:
            update = increment(value, -1);
            break;
        }
        setter.invoke(receiver, update);
        return value;
    }

    private static Object increment(Object value, int delta) {
        if (value instanceof Integer) {
            int val0 = (Integer) value;
            long val = (long)val0 + delta;
            int val1 = (int)val;
            if (val1 == val) {
                return val1;
            }
        }
        return incrementSlow(value, delta);
    }
    private static Object incrementSlow(Object value, int delta) {
        Object result;
        if (value instanceof Number) {
            result = ((Number)value).doubleValue() + delta;
        } else {
            result = ScriptRuntime.toNumber(value) + delta;
        }
        System.out.println("*** incrementSlow "+value+" + "+delta+" = "+result);
        return result;
    }

    protected static boolean checkLayout(ClassyLayout layout, Object receiver) {
        ClassyLayout rlo = null;
        if (receiver instanceof HasLayout) {
            rlo = ((HasLayout) receiver).getLayout();
            if (layout == rlo)  return true;
        }
        assert(!layout.equals(rlo));
        /*
        if (TRACE_SITES) {
            System.out.println("*** missed prediction on "+receiver);
            System.out.println("    expected: "+layout);
            System.out.println("    found:    "+rlo);
        }
        */
        return false;
    }
    // FIXME: Remove this cloned code when we get invokedynamic profiling.
    protected static boolean checkLayout_CS(ClassyLayout layout, Object receiver) {
        ClassyLayout rlo = null;
        if (receiver instanceof ClassyScriptable) {
            rlo = ((ClassyScriptable) receiver).getLayout();
            if (layout == rlo)  return true;
        }
        assert(!layout.equals(rlo));
        /*
        if (TRACE_SITES) {
            System.out.println("*** missed prediction on "+receiver);
            System.out.println("    expected: "+layout);
            System.out.println("    found:    "+rlo);
        }
        */
        return false;
    }

    protected static boolean checkPrototype(ClassyLayout layout, Object protoSlot, Object protoValue, Object receiver) {
        ClassyLayout rlo = null;
        if (receiver instanceof HasLayout) {
            rlo = ((HasLayout) receiver).getLayout();
            if (layout == rlo) {
                // Also check the prototype slot.
                Object current = ScriptRuntime.getObjectElem(layout.prototype(), protoSlot);
                if (current == protoValue)
                    return true;
                // This doesn't happen unless prototypes are mutated somehow.
                /*
                if (TRACE_SITES) {
                    System.out.println("*** missed prototype prediction on "+receiver);
                    System.out.println("    layout:   "+layout);
                    System.out.println("    slot:     "+protoSlot);
                    System.out.println("    expected: "+protoValue);
                    System.out.println("    found:    "+current);
                }
                */
                return false;
            }
        }
        assert(!layout.equals(rlo));
        // This happens routinely at polymorphic call sites.
        /*
        if (TRACE_SITES) {
            System.out.println("*** missed prediction on polymorphic "+receiver);
            System.out.println("    expected: "+layout);
            System.out.println("    found:    "+rlo);
            Thread.dumpStack();
        }
        */
        return false;
    }
    // FIXME: Remove this cloned code when we get invokedynamic profiling.
    protected static boolean checkPrototype_CS(ClassyLayout layout, Object protoSlot, Object protoValue, Object receiver) {
        ClassyLayout rlo = null;
        if (receiver instanceof ClassyScriptable) {
            rlo = ((ClassyScriptable) receiver).getLayout();
            if (layout == rlo) {
                // Also check the prototype slot.
                Object current = ScriptRuntime.getObjectElem(layout.prototype(), protoSlot);
                if (current == protoValue)
                    return true;
                // This doesn't happen unless prototypes are mutated somehow.
                /*
                if (TRACE_SITES) {
                    System.out.println("*** missed prototype prediction on "+receiver);
                    System.out.println("    layout:   "+layout);
                    System.out.println("    slot:     "+protoSlot);
                    System.out.println("    expected: "+protoValue);
                    System.out.println("    found:    "+current);
                }
                */
                return false;
            }
        }
        assert(!layout.equals(rlo));
        // This happens routinely at polymorphic call sites.
        /*
        if (TRACE_SITES) {
            System.out.println("*** missed prediction on polymorphic "+receiver);
            System.out.println("    expected: "+layout);
            System.out.println("    found:    "+rlo);
            Thread.dumpStack();
        }
        */
        return false;
    }

    // Profile state:
    TypeProfile typeProfile;
    int profileCount;  // number of profile steps
    int specializedMisses;  // number of type profile misses seen, after specialization

    public void resetProfile() {
        typeProfile = null;
        specializedMisses = 0;
        //profileCount = 0;  // do not reset this one
    }
    public int incrementProfileCount() {
        int cc1 = profileCount;
        int cc2 = cc1 + 1;
        if (cc2 <= 0)
            return cc1;  // no change after overflow
        profileCount = cc2;
        return cc2;
    }

    protected TypeProfile makeTypeProfile(Object receiver) {
        if (receiver == null) {
            return TypeProfile.forSingleton(null);
        } else if (receiver instanceof HasLayout) {
            return TypeProfile.forLayout(((HasLayout)receiver).getLayout());
        } else {
            return TypeProfile.forClass(receiver.getClass());
        }
    }

    /*
    MethodHandle computeGuard(TypeProfile prof) {
        return MethodHandles.insertArgument(MH_match, prof);
    }
    private static final MethodHandle MH_match = LOOKUP.
        findVirtual(TypeProfile.class, "match",
                    MethodType.methodType(boolean.class, Object.class));
    */

    protected MethodHandle[] computeGuardAndFastPath(TypeProfile prof) {
        if (prof instanceof TypeProfile.ForLayout) {
            ClassyLayout layout = ((TypeProfile.ForLayout) prof).matchLayout();
            if (!ClassyScriptable.USE_INDEXED_PROPERTIES && isIndexed())
                return null; // no fast path
            MethodHandle fastPath = null;
            Class<?> staticType = layout.staticType();  // Struct8 or some such
            Slot slot = layout.findSlot(propertyName);
            Object guardProtoValue = null;
            if (slot != null)
                assert(isIndexed() == (slot instanceof IndexedSlot));
            if (isIndexed())  throw new InternalError("indexed slots on layouts are untested");
            if (referenceKind == "get") {
                assert(arity() == 1 + (isIndexed() ? 1 : 0));
                if (slot != null) {
                    if (isIndexed())
                        fastPath = ((IndexedSlot)slot).safeIndexedGetter();
                    else
                        fastPath = slot.getter();
                } else {
                    MethodHandle MH_getFromPrototype;
                    // Could also use guardProtoValue here.  Worth it?
                    if (isIndexed()) {
                        return null;  // no fast path for this (yet)
                    } else if (propertyName instanceof String) {
                        MH_getFromPrototype = MH_getFromPrototype_String;
                    } else {
                        assert(propertyName instanceof Integer);
                        MH_getFromPrototype = MH_getFromPrototype_Integer;
                    }
                    fastPath = MethodHandles.insertArguments(MH_getFromPrototype, 0, layout.prototype(), propertyName);
                }
            } else if (referenceKind == "set") {
                assert(arity() == 2 + (isIndexed() ? 1 : 0));
                if (slot != null) {
                    if (isIndexed())
                        fastPath = ((IndexedSlot)slot).safeIndexedSetter();
                    else
                        fastPath = slot.setter();
                } else {
                    ClassyLayout layout2 = layout.extendWith(propertyName);
                    slot = layout2.lastSlot();
                    assert(layout2.findSlot(propertyName) == slot);
                    if (isIndexed())
                        fastPath = ((IndexedSlot)slot).safeIndexedSetter();
                    else
                        fastPath = slot.safeSetter();
                    if (false) {  // not yet: disturbs monomorphism downstream
                        ClassyLayout layout3 = layout2.guessExtension();
                        assert(layout3.findSlot(propertyName) == slot);
                        if (layout3 != layout2) {
                            if (TRACE_SITES)  System.out.println("Multiple extension: "+layout3+"; extra="+(layout3.length() - layout2.length()));
                        }
                        layout2 = layout3;
                    }
                    fastPath = MethodHandles.insertArguments(MH_fastExtendAndSet, 0, layout2, fastPath);
                }
            } else if (referenceKind == "call" || referenceKind == "apply") {
                int callArity = -1;  // number of regular (non-this) arguments, or -1 if not positional
                if (referenceKind == "call")
                    callArity = arity() - 1;
                if (slot != null) {
                    if (true)  throw new RuntimeException("NYI");
                    fastPath = MethodHandles.insertArguments(MH_fastApply_local, 0, slot);
                    callArity = -1; // FIXME
                } else {
                    Object proto = layout.prototype();
                    Object fn = null;
                    try {
                        fn = ScriptRuntime.getObjectElem(proto, propertyName);
                    } catch (RhinoException ignore) {
                    }
                    if (fn instanceof DynamicCallable) {
                        // First, try calling the callable directly through a MH.
                        MethodHandle callN = ((DynamicCallable)fn).callHandle();
                        if (callN == null && callArity >= 0 && callArity < MH_callDynamic.length) {
                            // Try calling DynamicCallable.callN on the interface.
                            callN = MH_callDynamic[callArity];
                        }
                        if (callN != null) {
                            fastPath = MethodHandles.insertArguments(callN, 0, fn);
                        } else {
                            callArity = -1;  // give up
                        }
                    }
                    if (fn instanceof Callable && fastPath == null) {
                        // will do this:  if (proto.slot == fn) fn.call...(arg...);
                        guardProtoValue = fn;
                        fastPath = MethodHandles.insertArguments(MH_fastApply_prototype, 0, fn);
                        callArity = -1;
                    }
                }
                if (fastPath != null && referenceKind == "call" && callArity == -1) {
                    // FIXME: positional argument passing is NYI
                    fastPath = MethodHandles.convertArguments(fastPath, type());
                }
            } else if (referenceKind.startsWith("incr@")) {
                assert(arity() == 1 + (isIndexed() ? 1 : 0));
                if (slot != null) {
                    int incrDecrMask = Integer.valueOf(referenceKind.substring("incr@".length()));
                    if (!isIndexed())
                        fastPath = MethodHandles.insertArguments(MH_fastIncrDecr, 0, slot.getter(), slot.setter(), incrDecrMask);
                    else
                        return null;  //FIXME!
                }
            } else if (referenceKind == "name") {
                assert(arity() == 1);
                if (slot != null) {
                    fastPath = slot.getter();
                } else {
                    assert(propertyName instanceof String);
                    fastPath = MethodHandles.insertArguments(MH_getFromParentScope, 0, layout.prototype(), propertyName);
                }
            } else if (referenceKind == "name=") {
                assert(arity() == 2);
            }
            if (fastPath != null) {
                MethodHandle guard;
                if (guardProtoValue == null) {
                    if (ClassyScriptable.class.isAssignableFrom(staticType))
                        guard = MethodHandles.insertArguments(MH_checkLayout_CS, 0, layout);  //FIXME: Remove
                    else
                        guard = MethodHandles.insertArguments(MH_checkLayout, 0, layout);
                } else {
                    if (ClassyScriptable.class.isAssignableFrom(staticType))
                        guard = MethodHandles.insertArguments(MH_checkPrototype_CS, 0, layout, propertyName, guardProtoValue);  //FIXME: Remove
                    else
                        guard = MethodHandles.insertArguments(MH_checkPrototype, 0, layout, propertyName, guardProtoValue);
                }
                return new MethodHandle[] { guard, fastPath };
            }
        }
        if (prof instanceof TypeProfile.ForClass &&
            FastPaths.class.isAssignableFrom(((TypeProfile.ForClass) prof).matchClass())) {
            MethodHandle fastPath = null;
            if (referenceKind == "get") {
                assert(arity() == 1 + (isIndexed() ? 1 : 0));
                if (propertyName instanceof String)
                    fastPath = FastPaths.MH_getObjectProp;
                else if (propertyName instanceof Integer)
                    fastPath = FastPaths.MH_getObjectIndex;
                else
                    fastPath = FastPaths.MH_getObjectElem;
            } else if (referenceKind == "set") {
                assert(arity() == 2 + (isIndexed() ? 1 : 0));
                if (propertyName instanceof String)
                    fastPath = FastPaths.MH_setObjectProp;
                else if (propertyName instanceof Integer)
                    fastPath = FastPaths.MH_setObjectIndex;
                else
                    fastPath = FastPaths.MH_setObjectElem;
            } else if (referenceKind.startsWith("incr@")) {
                assert(arity() == 1 + (isIndexed() ? 1 : 0));
                return null;  // FIXME!
            }
            if (fastPath != null) {
                if (!isIndexed())
                    fastPath = MethodHandles.insertArguments(fastPath, 1, propertyName);
                MethodHandle guard = MethodHandles.insertArguments(MH_isInstance, 0, FastPaths.class);
                return new MethodHandle[] { guard, fastPath };
            }
        }
        // Maybe specialize other types: Scriptable, String, Double, Integer, POJOs, etc.
        return null;
    }

    /** Compute the "slow path" for this access. */
    MethodHandle computeSlowPath() {
        // FIXME: Use switch(String) here.
        if (referenceKind == "get") {
            // SR.getObjectElem(_, name)
            if (isIndexed())
                return MH_slowGet;
            return MethodHandles.insertArguments(MH_slowGet, 1, propertyName);
        } else if (referenceKind == "set") {
            // SR.setObjectElem(_, name, __)
            if (isIndexed())
                return MH_slowSet;
            return MethodHandles.insertArguments(MH_slowSet, 1, propertyName);
        } else if (referenceKind == "name") {
            // SR.name(CurrentContext, _, name)
            return MethodHandles.insertArguments(MH_slowName, 0, this);
        } else if (referenceKind == "call" || referenceKind == "apply") {
            int callArity = -1;  // stands for "varargs"
            if (referenceKind == "call") {
                callArity = arity() - 1;
                assert(callArity >= 0);
            }
            MethodHandle getFnAndThis = MethodHandles.insertArguments(MH_getNamedFnAndThis, 0, propertyName);
            MethodHandle callFn = DynamicCallable.Methods.callHandle(CallableAndThis.class, callArity);
            //System.out.println("*** slow path for "+this+" arity="+callArity+" callFn="+callFn);
            MethodHandle target;
            if (callFn != null) {
                target = MethodHandles.filterArguments(callFn, 0, getFnAndThis);
            } else {
                // no direct path; this happened because callArity is too large
                MethodHandle applyFn = DynamicCallable.Methods.callHandle(Callable.class, -1);
                assert(callArity != -1 && applyFn != null);
                MethodHandle apply = MethodHandles.filterArguments(applyFn, 0, getFnAndThis);
                target = MethodHandles.collectArguments(apply, type());
            }
            //System.out.println("*** target="+target+target.type());
            return MethodHandles.convertArguments(target, type());
            // Really slow:
            //return MethodHandles.insertArguments(MH_slowApply, 0, this);
        } else if (referenceKind.startsWith("incr@")) {
            // SR.elemIncrDecr(_, name, CurrentContext, mask)
            int incrDecrMask = Integer.valueOf(referenceKind.substring("incr@".length()));
            return MethodHandles.insertArguments(MH_slowIncrDecr, 0, this, incrDecrMask);
        } else {
            throw new RuntimeException("not yet implemented: "+this);
        }
    }

    MethodHandle computeProfiler(boolean forMissPath) {
        MethodHandle profiler = (forMissPath ? MH_profileReceiverForMiss : MH_profileReceiver);
        profiler = MethodHandles.insertArguments(profiler, 0, this);
        Class<?> rtype = slowPath.type().parameterType(0);  // either Object or Scriptable
        profiler = MethodHandles.convertArguments(profiler, MethodType.methodType(rtype, rtype));
        return profiler;
    }

    /** The neutral state of a call site is the same as the megamorphic state,
     *  except that the call site's profiler routine is run over every incoming receiver (1st arg).
     *  This profiler routine collects history, and may relink the call site to something specialized.
     */
    public void setNeutral() {
        if (TRACE_SITES)  System.out.println("Neutral: "+this);
        specializedMisses = 0;
        setTarget(MethodHandles.filterArguments(slowPath, 0, computeProfiler(false)));
    }

    private static final Class[] NO_TYPES = { };

    static int MEGAMORPHIC_CASE_LIMIT = 0;  // decision trees don't work yet

    public void setSpecialized(TypeProfile allProfiles) {
        if (TRACE_SITES)  System.out.println((allProfiles.isMonomorphic()?"Monomorphic: ":"Polymorphic: ")+this);
        //if (!allProfiles.isMonomorphic())  System.out.println("Polymorphic profile: "+allProfiles);
        MethodType type = this.type();  // type of this call
        MethodHandle elsePath = slowPath;
        // add a profile step to the slow path:
        elsePath = MethodHandles.filterArguments(elsePath, 0, computeProfiler(true));
        TypeProfile[] cases = allProfiles.cases();
        int casesToCode = cases.length;
        if (casesToCode > 1 && casesToCode > MEGAMORPHIC_CASE_LIMIT) {
            elsePath = slowPath;  // turn off profiling
            casesToCode = MEGAMORPHIC_CASE_LIMIT;
        }
        for (int i = casesToCode - 1; i >= 0; i--) {
            MethodHandle[] guardAndFastPath = computeGuardAndFastPath(cases[i]);
            if (guardAndFastPath == null) {
                if (TRACE_SITES) {
                    System.out.println("Profile failed to code: "+cases[i]);
                    Thread.dumpStack();
                }
                continue;
            }
            MethodHandle guard = guardAndFastPath[0], fastPath = guardAndFastPath[1];
            assert(guard.type() == MethodType.methodType(boolean.class, Object.class));
            guard = MethodHandles.convertArguments(guard, guard.type().changeParameterType(0, type.parameterType(0)));
            fastPath = MethodHandles.convertArguments(fastPath, type);
            elsePath = MethodHandles.guardWithTest(guard, fastPath, elsePath);
        }
        setTarget(elsePath);
    }

    /** Set the target permanently to the slow path.
     *  There will be no more profiling.
     */
    public void setMegamorphic() {
        if (TRACE_SITES)  System.out.println("Megamorphic: "+this);
        setTarget(slowPath);
        resetProfile();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        if (profileCount != 0)
            buf.append("/count="+profileCount);
        if (specializedMisses != 0)
            buf.append("/misses="+specializedMisses);
        if (typeProfile != null)
            buf.append("/profile="+typeProfile);
        return buf.toString();
    }
}
