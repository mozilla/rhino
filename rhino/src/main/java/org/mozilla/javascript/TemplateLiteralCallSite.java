package org.mozilla.javascript;

import org.mozilla.classfile.DynamicConstant;

public class TemplateLiteralCallSite implements DynamicConstant {
    private volatile Scriptable callSite;
    private final Object[] values;
    private final String[] rawValues;

    public TemplateLiteralCallSite(Object[] values, String[] rawValues) {
        assert (values.length == rawValues.length);
        this.values = values;
        this.rawValues = rawValues;
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

        for (int i = 0; i < values.length; i++ ) {
            siteObj.put(i, siteObj, values[i]);

            rawObj.put(i, rawObj, rawValues[i]);
        }

        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, rawObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
        AbstractEcmaObjectOperations.setIntegrityLevel(
                cx, siteObj, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);

        return siteObj;
    }
}
