/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public class NativeSymbol extends IdScriptableObject {

    public static final String SPECIES_PROPERTY = "@@species";
    public static final String ITERATOR_PROPERTY = "@@iterator";
    public static final String TO_STRING_TAG_PROPERTY = "@@toStringTag";

    public static final String CLASS_NAME = "Symbol";

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeSymbol obj = new NativeSymbol();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    private NativeSymbol() {
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        super.fillConstructorProperties(ctor);
        ctor.defineProperty("iterator", ITERATOR_PROPERTY, DONTENUM | READONLY | PERMANENT);
        ctor.defineProperty("species", SPECIES_PROPERTY, DONTENUM | READONLY | PERMANENT);
        ctor.defineProperty("toStringTag", TO_STRING_TAG_PROPERTY, DONTENUM | READONLY | PERMANENT);
    }

    // #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id = 0;
// #generated# Last update: 2015-06-07 10:40:05 EEST
        L0: { id = 0; String X = null;
            if (s.length()==11) { X="constructor";id=Id_constructor; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor          = 1,
        MAX_PROTOTYPE_ID        = Id_constructor;

    // #/string_id_map#


    @Override
    protected void initPrototypeId(int id)
    {
        String s = null;
        int arity = -1;
        switch (id) {
            case Id_constructor:        arity = 0; s = "constructor"; break;
            default:                    super.initPrototypeId(id);
        }
        initPrototypeMethod(CLASS_NAME, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(CLASS_NAME)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                return new NativeSymbol();
        }
        return super.execIdCall(f, cx, scope, thisObj, args);

    }

    @Override
    public String getTypeOf() {
        return "symbol";
    }
}
