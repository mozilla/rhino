/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xmlimpl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

class XMLCtor {
    static final long serialVersionUID = -8708195078359817341L;

    private static final Object XMLCTOR_TAG = "XMLCtor";

    private static XmlProcessor options;

    //    private XMLLibImpl lib;

    private XMLCtor(XML xml, Object tag, int id, int arity) {
        // super(xml, tag, id, arity);
        //        this.lib = xml.lib;
        this.options = xml.getProcessor();
    }

    public static Scriptable init() {
        return null;
    }

    private static void createProperties(Context cx, LambdaConstructor obj) {
        // obj.definePrototypeMethod(cx, "defaultSettings", null, null, 0);
        // obj.definePrototypeMethod(cx, "settings", null, null, 0);
        // obj.definePrototypeMethod(cx, "setSettings", null, null, 0);
        obj.defineProperty(
                cx,
                "prettyPrinting",
                (o) -> ScriptRuntime.wrapBoolean(options.isPrettyPrinting()),
                (o, v) -> options.setPrettyPrinting(ScriptRuntime.toBoolean(v)),
                0);
        obj.defineProperty(
                cx,
                "prettyIndent",
                (o) -> ScriptRuntime.wrapInt(options.getPrettyIndent()),
                (o, v) -> options.setPrettyIndent(ScriptRuntime.toInt32(v)),
                0);
        obj.defineProperty(
                cx,
                "ignoreWhitespace",
                (o) -> ScriptRuntime.wrapBoolean(options.isIgnoreWhitespace()),
                (o, v) -> options.setIgnoreWhitespace(ScriptRuntime.toBoolean(v)),
                0);
        obj.defineProperty(
                cx,
                "ignoreProcessingInstructions",
                (o) -> ScriptRuntime.wrapBoolean(options.isIgnoreProcessingInstructions()),
                (o, v) -> options.setIgnoreProcessingInstructions(ScriptRuntime.toBoolean(v)),
                0);
        obj.defineProperty(
                cx,
                "ignoreComments",
                (o) -> ScriptRuntime.wrapBoolean(options.isIgnoreComments()),
                (o, v) -> options.setIgnoreComments(ScriptRuntime.toBoolean(v)),
                0);
    }

    // private void writeSetting(Scriptable target) {
    //     for (int i = 1; i <= MAX_INSTANCE_ID; ++i) {
    //         int id = super.getMaxInstanceId() + i;
    //         String name = getInstanceIdName(id);
    //         Object value = getInstanceIdValue(id);
    //         ScriptableObject.putProperty(target, name, value);
    //     }
    // }

    // private void readSettings(Scriptable source) {
    //     for (int i = 1; i <= MAX_INSTANCE_ID; ++i) {
    //         int id = super.getMaxInstanceId() + i;
    //         String name = getInstanceIdName(id);
    //         Object value = ScriptableObject.getProperty(source, name);
    //         if (value == Scriptable.NOT_FOUND) {
    //             continue;
    //         }
    //         switch (i) {
    //             case Id_ignoreComments:
    //             case Id_ignoreProcessingInstructions:
    //             case Id_ignoreWhitespace:
    //             case Id_prettyPrinting:
    //                 if (!(value instanceof Boolean)) {
    //                     continue;
    //                 }
    //                 break;
    //             case Id_prettyIndent:
    //                 if (!(value instanceof Number)) {
    //                     continue;
    //                 }
    //                 break;
    //             default:
    //                 throw new IllegalStateException();
    //         }
    //         setInstanceIdValue(id, value);
    //     }
    // }

    // #string_id_map#

    private static final int Id_ignoreComments = 1,
            Id_ignoreProcessingInstructions = 2,
            Id_ignoreWhitespace = 3,
            Id_prettyIndent = 4,
            Id_prettyPrinting = 5,
            MAX_INSTANCE_ID = 5;

    // public Object execIdCall(
    //         IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
    // {
    //     if (!f.hasTag(XMLCTOR_TAG)) {
    //         return super.execIdCall(f, cx, scope, thisObj, args);
    //     }
    //     int id = f.methodId();
    //     switch (id) {
    //         case Id_defaultSettings:
    //             {
    //                 options.setDefault();
    //                 Scriptable obj = cx.newObject(scope);
    //                 writeSetting(obj);
    //                 return obj;
    //             }
    //         case Id_settings:
    //             {
    //                 Scriptable obj = cx.newObject(scope);
    //                 writeSetting(obj);
    //                 return obj;
    //             }
    //         case Id_setSettings:
    //             {
    //                 if (args.length == 0 || args[0] == null || args[0] == Undefined.instance) {
    //                     options.setDefault();
    //                 } else if (args[0] instanceof Scriptable) {
    //                     readSettings((Scriptable) args[0]);
    //                 }
    //                 return Undefined.instance;
    //             }
    //     }
    //     throw new IllegalArgumentException(String.valueOf(id));
    // }

    // /** hasInstance for XML objects works differently than other objects; see ECMA357 13.4.3.10.
    // */
    // @Override
    // public boolean hasInstance(Scriptable instance) {
    //     return (instance instanceof XML || instance instanceof XMLList);
    // }
}
