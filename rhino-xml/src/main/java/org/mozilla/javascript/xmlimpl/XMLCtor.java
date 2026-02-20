/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xmlimpl;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SerializableCallable;
import org.mozilla.javascript.Undefined;

class XMLCtor {
    public static void createProperties(
            Context cx, ScriptableObject scope, ScriptableObject obj, XMLLibImpl lib) {
        defineMethod(
                obj,
                scope,
                "defaultSettings",
                0,
                (lcx, lscope, lthisObj, largs) -> {
                    lib.getProcessor().setDefault();
                    var res = lcx.newObject(lscope);
                    writeSetting(obj, res);
                    return res;
                });
        defineMethod(
                obj,
                scope,
                "settings",
                0,
                (lcx, lscope, lthisObj, largs) -> {
                    var res = lcx.newObject(lscope);
                    writeSetting(obj, res);
                    return res;
                });
        defineMethod(
                obj,
                scope,
                "setSettings",
                1,
                (lcx, lscope, lthisObj, largs) -> {
                    if (largs.length == 0 || largs[0] == null || largs[0] == Undefined.instance) {
                        lib.getProcessor().setDefault();
                    } else if (largs[0] instanceof Scriptable) {
                        readSettings(obj, (Scriptable) largs[0]);
                    }
                    return Undefined.instance;
                });

        ScriptableObject.defineBuiltInProperty(
                obj,
                "prettyPrinting",
                0,
                (b, s) -> ScriptRuntime.wrapBoolean(lib.getProcessor().isPrettyPrinting()),
                (b, v, o, s, t) -> {
                    lib.getProcessor().setPrettyPrinting(ScriptRuntime.toBoolean(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "prettyIndent",
                0,
                (b, s) -> ScriptRuntime.wrapInt(lib.getProcessor().getPrettyIndent()),
                (b, v, o, s, t) -> {
                    lib.getProcessor().setPrettyIndent(ScriptRuntime.toInt32(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "ignoreWhitespace",
                0,
                (b, s) -> ScriptRuntime.wrapBoolean(lib.getProcessor().isIgnoreWhitespace()),
                (b, v, o, s, t) -> {
                    lib.getProcessor().setIgnoreWhitespace(ScriptRuntime.toBoolean(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "ignoreProcessingInstructions",
                0,
                (b, s) ->
                        ScriptRuntime.wrapBoolean(
                                lib.getProcessor().isIgnoreProcessingInstructions()),
                (b, v, o, s, t) -> {
                    lib.getProcessor().setIgnoreProcessingInstructions(ScriptRuntime.toBoolean(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "ignoreComments",
                0,
                (b, s) -> ScriptRuntime.wrapBoolean(lib.getProcessor().isIgnoreComments()),
                (b, v, o, s, t) -> {
                    lib.getProcessor().setIgnoreComments(ScriptRuntime.toBoolean(v));
                    return true;
                });
    }

    private static void defineMethod(
            ScriptableObject obj,
            ScriptableObject scope,
            String name,
            int length,
            SerializableCallable target) {
        defineMethod(obj, scope, name, length, target, DONTENUM, DONTENUM | READONLY, true);
    }

    private static void defineMethod(
            ScriptableObject obj,
            Scriptable scope,
            String name,
            int length,
            SerializableCallable target,
            int attributes,
            int propertyAttributes,
            boolean defaultPrototype) {
        LambdaFunction f = new LambdaFunction(scope, name, length, target, defaultPrototype);
        f.setStandardPropertyAttributes(propertyAttributes);
        obj.defineProperty(name, f, attributes);
    }

    private static void writeSetting(ScriptableObject thisObj, Scriptable target) {
        for (var p : propNames) {
            Object value = thisObj.get(p, thisObj);
            ScriptableObject.putProperty(target, p, value);
        }
    }

    private static void readSettings(ScriptableObject thisObj, Scriptable source) {
        for (var p : propNames) {
            Object value = ScriptableObject.getProperty(source, p);
            if (value == Scriptable.NOT_FOUND) {
                continue;
            }
            switch (p) {
                case "ignoreComments":
                case "ignoreProcessingInstructions":
                case "ignoreWhitespace":
                case "prettyPrinting":
                    if (!(value instanceof Boolean)) {
                        continue;
                    }
                    break;
                case "prettyIndent":
                    if (!(value instanceof Number)) {
                        continue;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
            ScriptableObject.putProperty(thisObj, p, value);
        }
    }

    // #string_id_map#

    private static final String[] propNames =
            new String[] {
                "ignoreComments",
                "ignoreProcessingInstructions",
                "ignoreWhitespace",
                "prettyIndent",
                "prettyPrinting"
            };
}
