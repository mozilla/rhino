/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Default implementation of template literal proxy. This implementation contains the template
 * literal logic to reduce the complexity of the interpreter loop and fix Android JIT compilation
 * issues.
 *
 * @author Anivar Aravind
 */
public class DefaultTemplateLiteralProxy implements TemplateLiteralProxy {

    @Override
    public Scriptable getCallSite(Context cx, Scriptable scope, Object[] strings, int index) {
        Object callsite = strings[index];

        if (callsite instanceof Scriptable) return (Scriptable) callsite;

        assert callsite instanceof String[];
        String[] vals = (String[]) callsite;
        assert (vals.length & 1) == 0;

        ScriptableObject siteObj = (ScriptableObject) cx.newArray(scope, vals.length >>> 1);
        ScriptableObject rawObj = (ScriptableObject) cx.newArray(scope, vals.length >>> 1);

        siteObj.put("raw", siteObj, rawObj);
        siteObj.setAttributes("raw", ScriptableObject.DONTENUM);

        for (int i = 0, n = vals.length; i < n; i += 2) {
            int idx = i >>> 1;
            siteObj.put(idx, siteObj, (vals[i] == null ? Undefined.instance : vals[i]));

            rawObj.put(idx, rawObj, vals[i + 1]);
        }

        // Freeze the objects - since we can't access INTEGRITY_LEVEL from outside package,
        // we use the public preventExtensions method and make properties non-configurable
        rawObj.preventExtensions();
        siteObj.preventExtensions();

        // Make all properties non-configurable and non-writable
        for (Object id : rawObj.getIds()) {
            if (id instanceof Integer) {
                rawObj.setAttributes(
                        (Integer) id, ScriptableObject.READONLY | ScriptableObject.PERMANENT);
            }
        }

        for (Object id : siteObj.getIds()) {
            if (id instanceof Integer) {
                siteObj.setAttributes(
                        (Integer) id, ScriptableObject.READONLY | ScriptableObject.PERMANENT);
            } else if ("raw".equals(id)) {
                siteObj.setAttributes(
                        "raw",
                        ScriptableObject.READONLY
                                | ScriptableObject.PERMANENT
                                | ScriptableObject.DONTENUM);
            }
        }

        strings[index] = siteObj;

        return siteObj;
    }
}
