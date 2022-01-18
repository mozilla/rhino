/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public class ScriptRuntimeES6 {

    public static Object requireObjectCoercible(
            Context cx, Object val, IdFunctionObject idFuncObj) {
        if (val == null || Undefined.isUndefined(val)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.called.null.or.undefined",
                    idFuncObj.getTag(),
                    idFuncObj.getFunctionName());
        }
        return val;
    }
}
