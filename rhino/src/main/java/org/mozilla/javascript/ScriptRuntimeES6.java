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

    public static Object requireObjectCoercible(
            Context cx, Object val, Object tag, String functionName) {
        if (val == null || Undefined.isUndefined(val)) {
            throw ScriptRuntime.typeErrorById("msg.called.null.or.undefined", tag, functionName);
        }
        return val;
    }

    /** Registers the symbol <code>[Symbol.species]</code> on the given constructor function. */
    public static void addSymbolSpecies(
            Context cx, Scriptable scope, ScriptableObject constructor) {
        ScriptableObject speciesDescriptor = (ScriptableObject) cx.newObject(scope);
        ScriptableObject.putProperty(speciesDescriptor, "enumerable", false);
        ScriptableObject.putProperty(speciesDescriptor, "configurable", true);
        ScriptableObject.putProperty(
                speciesDescriptor,
                "get",
                new LambdaFunction(
                        scope,
                        "get [Symbol.species]",
                        0,
                        (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                                thisObj));
        constructor.defineOwnProperty(cx, SymbolKey.SPECIES, speciesDescriptor, false);
    }

    /** Registers the symbol <code>[Symbol.unscopables]</code> on the given constructor function. */
    public static void addSymbolUnscopables(
            Context cx, Scriptable scope, ScriptableObject constructor, LazilyLoadedCtor value) {
        constructor.addLazilyInitializedValue(
                SymbolKey.UNSCOPABLES,
                0,
                value,
                ScriptableObject.DONTENUM | ScriptableObject.READONLY);
    }
}
