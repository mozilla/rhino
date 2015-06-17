/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public class ScriptRuntimeES6 {

    public static Scriptable requireObjectCoercible(Context cx, Scriptable val, IdFunctionObject idFuncObj) {
        if (cx.getLanguageVersion() < Context.VERSION_ES6) throw Context.reportRuntimeError(idFuncObj.getFunctionName() + " is only available for version >= ES6");

        if (val == null || val == Undefined.instance) throw ScriptRuntime.typeError2("msg.called.null.or.undefined", idFuncObj.getTag(), idFuncObj.getFunctionName());

        return val;
    }
}
