package org.mozilla.javascript;

/**
 * Abstract Object Operations as defined by EcmaScript
 * 
 * @see <a href="https://262.ecma-international.org/11.0/#sec-operations-on-objects">Abstract Operations - Operations on Objects</a>
 */
class AbstractEcmaObjectOperations {
    static enum INTEGRITY_LEVEL {
        FROZEN,
        SEALED
    }

    /**
     * Implementation of Abstract Object operation testIntegrityLevel as defined by EcmaScript
     * 
     * @param o
     * @param level
     * 
     * @return boolean
     * 
     * @see <a href="https://262.ecma-international.org/11.0/#sec-testintegritylevel">TestIntegrityLevel</a>
     */
    static boolean testIntegrityLevel (Object o, INTEGRITY_LEVEL level ) {
        ScriptableObject obj = ScriptableObject.ensureScriptableObject(o);
        Context cx = Context.getCurrentContext();

        if (obj.isExtensible()) return Boolean.FALSE;

        for (Object name : obj.getIds(true, true)) {
            ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
            if (Boolean.TRUE.equals(desc.get("configurable"))) return Boolean.FALSE;
            
            if (level == INTEGRITY_LEVEL.FROZEN && desc.isDataDescriptor(desc) && Boolean.TRUE.equals(desc.get("writable")))
                return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    /**
     * Implementation of Abstract Object operation setIntegrityLevel as defined by EcmaScript
     * 
     * Implementation currently tightly coupled with ScriptableObject impl.
     * which allow some optimization.
     * 
     * @param o
     * @param level
     * 
     * @return boolean
     * 
     * @see <a href="https://262.ecma-international.org/11.0/#sec-setintegritylevel">SetIntegrityLevel</a>
     */
    static boolean setIntegrityLevel (Object o, INTEGRITY_LEVEL level ) {
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
            - Step 3 calls for the .preventExtensions() before updating the propertyDescriptors,
              In Rhino however .preventExtensions() never returns false
              and calling it before will block updating the propertyDescriptors afterwards
            - While steps 6.a.i and 7.b.ii.3 call for the Abstract DefinePropertyOrThrow operation,
              the conditions under which a throw would occur aren't applicable when freezing or sealing an object
         */
         ScriptableObject obj = ScriptableObject.ensureScriptableObject(o);
         Context cx = Context.getCurrentContext();

         for (Object key : obj.getIds(true, true)) {
             ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, key);

             if (level == INTEGRITY_LEVEL.SEALED) {
                 if (Boolean.TRUE.equals(desc.get("configurable"))) {
                     desc.put("configurable", desc, Boolean.FALSE);

                     obj.defineOwnProperty(cx, key, desc, false);
                 }
             } else {
                 if (obj.isDataDescriptor(desc) && Boolean.TRUE.equals(desc.get("writable"))) {
                     desc.put("writable", desc, Boolean.FALSE);
                 }
                 if (Boolean.TRUE.equals(desc.get("configurable"))) {
                     desc.put("configurable", desc, Boolean.FALSE);
                 }
                 obj.defineOwnProperty(cx, key, desc, false);
             }
         }

         obj.preventExtensions();

         return true;
    }
    
    static boolean definePropertyOrThrow(Object o, Object p, Scriptable desc) {
        return true;
    }
}
