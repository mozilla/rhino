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
    static enum INTEGRITY_LEVEL {
        FROZEN,
        SEALED
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
                    && desc.isDataDescriptor(desc)
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
                    desc.put("configurable", desc, false);

                    obj.defineOwnProperty(cx, key, desc, false);
                }
            } else {
                if (obj.isDataDescriptor(desc) && Boolean.TRUE.equals(desc.get("writable"))) {
                    desc.put("writable", desc, false);
                }
                if (Boolean.TRUE.equals(desc.get("configurable"))) {
                    desc.put("configurable", desc, false);
                }
                obj.defineOwnProperty(cx, key, desc, false);
            }
        }

        return true;
    }
}
