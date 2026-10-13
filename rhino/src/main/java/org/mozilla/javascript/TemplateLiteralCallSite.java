package org.mozilla.javascript;

import java.lang.invoke.MethodHandles;

public class TemplateLiteralCallSite {
    /*
     * The values and raw values are each encoded as a single string so that they can be carried
     * as bootstrap arguments. Components are separated by SEPARATOR. Within a string component
     * SEPARATOR is written as ESCAPE '0' and ESCAPE as ESCAPE '1', so a component consisting of a
     * lone ESCAPE can only mean Undefined.instance, and an empty component the empty string.
     */
    private volatile Scriptable callSite;
    private final Object[] values;
    private final String[] rawValues;

    public TemplateLiteralCallSite(Object[] values, String[] rawValues) {
        assert (values.length == rawValues.length);
        this.values = values;
        this.rawValues = rawValues;
    }

    public Object[] encoded() {
        var vals = new Object[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            vals[i * 2] = rawValues[i];
            vals[i * 2 + 1] = values[i];
        }
        return vals;
    }

    public static TemplateLiteralCallSite templateLiteralCallSiteConstant(
            MethodHandles.Lookup lookup,
            String name,
            Class<?> type,
            int dedupHash,
            Object... things) {
        String[] rawValues = new String[things.length / 2];
        Object[] values = new Object[things.length / 2];
        for (int i = 0; i < rawValues.length; i++) {
            rawValues[i] = (String) things[i * 2];
            values[i] = things[i * 2 + 1];
        }
        return new TemplateLiteralCallSite(values, rawValues);
    }

    public Scriptable getSiteObject(Context cx, VarScope scope) {
        if (callSite == null) {
            synchronized (this) {
                if (callSite == null) {
                    callSite = initSiteObject(cx, scope);
                }
            }
        }
        return callSite;
    }

    public Scriptable initSiteObject(Context cx, VarScope scope) {
        ScriptableObject siteObj = (ScriptableObject) cx.newArray(scope, values.length);
        ScriptableObject rawObj = (ScriptableObject) cx.newArray(scope, rawValues.length);

        siteObj.put("raw", siteObj, rawObj);
        siteObj.setAttributes("raw", ScriptableObject.DONTENUM);

        for (int i = 0; i < values.length; i++) {
            siteObj.put(i, siteObj, values[i]);

            rawObj.put(i, rawObj, rawValues[i]);
        }

        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, rawObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, siteObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);

        return siteObj;
    }

    @Override
    public String toString() {
        return "TemplateLiteral('" + String.join("','", rawValues) + "')";
    }
}
