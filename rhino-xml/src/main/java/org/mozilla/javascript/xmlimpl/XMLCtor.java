/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xmlimpl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SerializableCallable;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;

class XMLCtor extends IdFunctionObject {
    static final long serialVersionUID = -8708195078359817341L;

    private static final Object XMLCTOR_TAG = "XMLCtor";

    private XmlProcessor options;

    private XMLLibImpl lib;

    private final boolean isXMLType; // to distinguish between XML and XMLList

    public XMLCtor(
            Context cx,
            ScriptableObject scope,
            XMLObjectImpl xmlObj,
            Object tag,
            int id,
            int arity) {
        super(xmlObj, tag, id, arity);
        //       this.lib = xmlObj.lib;
        this.options = xmlObj.getProcessor();
        this.isXMLType = xmlObj instanceof XML;
        createProperties(cx, scope, this);
    }

    public static IdFunctionObject init() {
        return null;
    }

    private static void createProperties(Context cx, ScriptableObject scope, XMLCtor obj) {
        // E4X instanceof semantics (ECMA-357 13.4.3.10)
        obj.defineProperty(
                SymbolKey.HAS_INSTANCE,
                new LambdaFunction(
                        scope,
                        "[Symbol.hasInstance]",
                        1,
                        (lcx, lscope, lthisObj, largs) -> {
                            if (largs.length > 0 && largs[0] instanceof Scriptable) {
                                return obj.hasInstance((Scriptable) largs[0]);
                            }
                            return false;
                        }),
                DONTENUM | READONLY | PERMANENT);

        defineMethod(
                obj,
                scope,
                "defaultSettings",
                0,
                (lcx, lscope, lthisObj, largs) -> {
                    obj.options.setDefault();
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
                        obj.options.setDefault();
                    } else if (largs[0] instanceof Scriptable) {
                        readSettings(obj, (Scriptable) largs[0]);
                    }
                    return Undefined.instance;
                });

        ScriptableObject.defineBuiltInProperty(
                obj,
                "prettyPrinting",
                0,
                (b, s) -> ScriptRuntime.wrapBoolean(b.options.isPrettyPrinting()),
                (b, v, o, s, t) -> {
                    b.options.setPrettyPrinting(ScriptRuntime.toBoolean(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "prettyIndent",
                0,
                (b, s) -> ScriptRuntime.wrapInt(b.options.getPrettyIndent()),
                (b, v, o, s, t) -> {
                    b.options.setPrettyIndent(ScriptRuntime.toInt32(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "ignoreWhitespace",
                0,
                (b, s) -> ScriptRuntime.wrapBoolean(b.options.isIgnoreWhitespace()),
                (b, v, o, s, t) -> {
                    b.options.setIgnoreWhitespace(ScriptRuntime.toBoolean(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "ignoreProcessingInstructions",
                0,
                (b, s) -> ScriptRuntime.wrapBoolean(b.options.isIgnoreProcessingInstructions()),
                (b, v, o, s, t) -> {
                    b.options.setIgnoreProcessingInstructions(ScriptRuntime.toBoolean(v));
                    return true;
                });
        ScriptableObject.defineBuiltInProperty(
                obj,
                "ignoreComments",
                0,
                (b, s) -> ScriptRuntime.wrapBoolean(b.options.isIgnoreComments()),
                (b, v, o, s, t) -> {
                    b.options.setIgnoreComments(ScriptRuntime.toBoolean(v));
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

    private static void writeSetting(XMLCtor thisObj, Scriptable target) {
        for (var p : propNames) {
            Object value = thisObj.get(p, thisObj);
            ScriptableObject.putProperty(target, p, value);
        }
    }

    private static void readSettings(XMLCtor thisObj, Scriptable source) {
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

    /** hasInstance for XML objects works differently than other objects; see ECMA357 13.4.3.10. */
    @Override
    public boolean hasInstance(Scriptable instance) {
        if (isXMLType) {
            // XML ctor: true for both XML and XMLList
            return (instance instanceof XML || instance instanceof XMLList);
        } else {
            // XMLList ctor: true only for XMLList
            return instance instanceof XMLList;
        }
    }
}
