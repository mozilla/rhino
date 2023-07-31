package org.mozilla.javascript;

/**
 * Abstract Object Operations as defined by EcmaScript
 *
 * @see <a href="https://262.ecma-international.org/11.0/#sec-operations-on-objects">Abstract
 *     Operations - Operations on Objects</a>
 *     <p>Notes
 *     <ul>
 *       <li>all methods are to deviate from the method signature defined in the EcmaScript
 *           specification, by taking an additional 1st parameter of type Context: (downstream)
 *           methods may need the Context object to read flags and we want to avoid having to look
 *           up the current context (for performance reasons)
 *       <li>all methods that implement an Abstract Operation as defined by EcmaScript are to be
 *           package-scopes methods, to prevent them from being used directly by 3rd party code,
 *           which would hamper evolving them over time to adept to newer EcmaScript specifications
 *       <li>a link to the method specification of the specific (EcmaScript) version implemented
 *           will be put in the JavaDoc of each method that implements an Abstract Operations
 *     </ul>
 */
class AbstractEcmaObjectOperations {
    enum INTEGRITY_LEVEL {
        FROZEN,
        SEALED
    }

    /**
     * Implementation of Abstract Object operation HasOwnProperty as defined by EcmaScript
     *
     * @param cx
     * @param o
     * @param property
     * @return boolean
     * @see <a href="https://262.ecma-international.org/12.0/#sec-hasownproperty"></a>
     */
    static boolean hasOwnProperty(Context cx, Object o, Object property) {
        Scriptable obj = ScriptableObject.ensureScriptable(o);
        boolean result;
        if (property instanceof Symbol) {
            result = ScriptableObject.ensureSymbolScriptable(o).has((Symbol) property, obj);
        } else {
            ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(property);
            if (s.stringId == null) {
                result = obj.has(s.index, obj);
            } else {
                result = obj.has(s.stringId, obj);
            }
        }

        return result;
    }

    /**
     * Implementation of Abstract Object operation testIntegrityLevel as defined by EcmaScript
     *
     * @param cx
     * @param o
     * @param level
     * @return boolean
     * @see <a
     *     href="https://262.ecma-international.org/11.0/#sec-testintegritylevel">TestIntegrityLevel</a>
     */
    static boolean testIntegrityLevel(Context cx, Object o, INTEGRITY_LEVEL level) {
        ScriptableObject obj = ScriptableObject.ensureScriptableObject(o);

        if (obj.isExtensible()) return false;

        for (Object name : obj.getIds(true, true)) {
            ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
            if (Boolean.TRUE.equals(desc.get("configurable"))) return false;

            if (level == INTEGRITY_LEVEL.FROZEN
                    && ScriptableObject.isDataDescriptor(desc)
                    && Boolean.TRUE.equals(desc.get("writable"))) return false;
        }

        return true;
    }

    /**
     * Implementation of Abstract Object operation setIntegrityLevel as defined by EcmaScript
     *
     * @param cx
     * @param o
     * @param level
     * @return boolean
     * @see <a
     *     href="https://262.ecma-international.org/11.0/#sec-setintegritylevel">SetIntegrityLevel</a>
     */
    static boolean setIntegrityLevel(Context cx, Object o, INTEGRITY_LEVEL level) {
        /*
           1. Assert: Type(O) is Object.
           2. Assert: level is either sealed or frozen.
           3. Let status be ? O.[[PreventExtensions]]().
           4. If status is false, return false.
           5. Let keys be ? O.[[OwnPropertyKeys]]().
           6. If level is sealed, then
               a. For each element k of keys, do
                   i. Perform ? DefinePropertyOrThrow(O, k, PropertyDescriptor { [[Configurable]]: false }).
           7. Else,
               a. Assert: level is frozen.
               b. For each element k of keys, do
                   i. Let currentDesc be ? O.[[GetOwnProperty]](k).
                   ii. If currentDesc is not undefined, then
                       1. If IsAccessorDescriptor(currentDesc) is true, then
                           a. Let desc be the PropertyDescriptor { [[Configurable]]: false }.
                       2. Else,
                           a. Let desc be the PropertyDescriptor { [[Configurable]]: false, [[Writable]]: false }.
                       3. Perform ? DefinePropertyOrThrow(O, k, desc).
           8. Return true.

           NOTES
           - While steps 6.a.i and 7.b.ii.3 call for the Abstract DefinePropertyOrThrow operation,
             the conditions under which a throw would occur aren't applicable when freezing or sealing an object,
             see https://262.ecma-international.org/11.0/#sec-validateandapplypropertydescriptor:
             1. n/a
             2. current cannot be undefined, because the logic operates only on existing properties
             3. n/a
             4. this code doesn't ever set configurable to true or modifies the enumerable property
             5. n/a
             6. as current and desc start out the same and the writable property is set only after checking if isDataDescriptor == true, this condition cannot occur
             7. both conditions under which false would be returned cannot occur here
             8. both conditions under which false would be returned cannot occur here
        */
        ScriptableObject obj = ScriptableObject.ensureScriptableObject(o);

        // TODO check .preventExtensions() return value once implemented and act accordingly to spec
        obj.preventExtensions();

        for (Object key : obj.getIds(true, true)) {
            ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, key);

            if (level == INTEGRITY_LEVEL.SEALED) {
                if (Boolean.TRUE.equals(desc.get("configurable"))) {
                    desc.put("configurable", desc, Boolean.FALSE);

                    obj.defineOwnProperty(cx, key, desc, false);
                }
            } else {
                if (ScriptableObject.isDataDescriptor(desc)
                        && Boolean.TRUE.equals(desc.get("writable"))) {
                    desc.put("writable", desc, Boolean.FALSE);
                }
                if (Boolean.TRUE.equals(desc.get("configurable"))) {
                    desc.put("configurable", desc, Boolean.FALSE);
                }
                obj.defineOwnProperty(cx, key, desc, false);
            }
        }

        return true;
    }

    /**
     * Implement the ECMAScript abstract operation "SpeciesConstructor" defined in section 7.2.33 of
     * ECMA262.
     *
     * @param cx context
     * @param s the object for which we will find the "species constructor" as per the spec
     * @param defaultConstructor as per the spec, the value that will be returned if there is no
     *     constructor on "s" or if the "species" symbol is not set.
     * @see <a href="https://tc39.es/ecma262/#sec-speciesconstructor"></a>
     */
    static Constructable speciesConstructor(
            Context cx, Scriptable s, Constructable defaultConstructor) {
        /*
        The abstract operation SpeciesConstructor takes arguments O (an Object) and
        defaultConstructor (a constructor). It is used to retrieve the constructor that should
        be used to create new objects that are derived from O. defaultConstructor is the
        constructor to use if a constructor @@species property cannot be found starting from O.
        It performs the following steps when called:

        1. Assert: Type(O) is Object.
        2. Let C be ? Get(O, "constructor").
        3. If C is undefined, return defaultConstructor.
        4. If Type(C) is not Object, throw a TypeError exception.
        5. Let S be ? Get(C, @@species).
        6. If S is either undefined or null, return defaultConstructor.
        7. If IsConstructor(S) is true, return S.
        8. Throw a TypeError exception.
         */
        Object constructor = ScriptableObject.getProperty(s, "constructor");
        if (constructor == Scriptable.NOT_FOUND || Undefined.isUndefined(constructor)) {
            return defaultConstructor;
        }
        if (!ScriptRuntime.isObject(constructor)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.arg.not.object", ScriptRuntime.typeof(constructor));
        }
        Object species = ScriptableObject.getProperty((Scriptable) constructor, SymbolKey.SPECIES);
        if (species == Scriptable.NOT_FOUND || species == null || Undefined.isUndefined(species)) {
            return defaultConstructor;
        }
        if (!(species instanceof Constructable)) {
            throw ScriptRuntime.typeErrorById("msg.not.ctor", ScriptRuntime.typeof(species));
        }
        return (Constructable) species;
    }

    /**
     * Set ( O, P, V, Throw)
     *
     * <p>https://262.ecma-international.org/12.0/#sec-set-o-p-v-throw
     */
    static void put(Context cx, Scriptable o, String p, Object v, boolean isThrow) {
        Scriptable base = ScriptableObject.getBase(o, p);
        if (base == null) base = o;

        if (base instanceof ScriptableObject) {
            if (((ScriptableObject) base).putImpl(p, 0, o, v, isThrow)) return;

            o.put(p, o, v);
        } else {
            base.put(p, o, v);
        }
    }

    /**
     * Set ( O, P, V, Throw)
     *
     * <p>https://262.ecma-international.org/12.0/#sec-set-o-p-v-throw
     */
    static void put(Context cx, Scriptable o, int p, Object v, boolean isThrow) {
        Scriptable base = ScriptableObject.getBase(o, p);
        if (base == null) base = o;

        if (base instanceof ScriptableObject) {
            if (((ScriptableObject) base).putImpl(null, p, o, v, isThrow)) return;

            o.put(p, o, v);
        } else {
            base.put(p, o, v);
        }
    }
}
