/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;

/**
 * This class implements ArrayIterator and StringIterator objects. See
 * http://wiki.ecmascript.org/doku.php?id=harmony:iterators
 */
public final class NativeElementIterator extends IdScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final Object ELEMENT_ITERATOR_TAG = "ElementIterator";

    static void init(ScriptableObject scope, boolean sealed) {
        // NativeElementIterator
        // Can't use "NativeElementIterator().exportAsJSClass" since we don't want
        // to define "NativeElementIterator" as a constructor in the top-level scope.
        NativeElementIterator prototype = new NativeElementIterator();
        if (scope != null) {
            prototype.setParentScope(scope);
            prototype.setPrototype(getObjectPrototype(scope));
        }
        prototype.activatePrototypeMap(MAX_PROTOTYPE_ID);
        if (sealed) {
            prototype.sealObject();
        }

        // Need to access Iterator prototype when constructing
        // Iterator instances, but don't have a iterator constructor
        // to use to find the prototype. Use the "associateValue"
        // approach instead.
        if (scope != null) {
            scope.associateValue(ELEMENT_ITERATOR_TAG, prototype);
        }
    }

    /**
     * Only for constructing the prototype object.
     */
    private NativeElementIterator() {
    }

    NativeElementIterator(Scriptable scope, Scriptable arrayLike) {
        this.arrayLike = arrayLike;
        this.index = 0;
        // Set parent and prototype properties. Since we don't have a
        // "Iterator" constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        NativeElementIterator prototype = (NativeElementIterator)
            ScriptableObject.getTopScopeValue(top, ELEMENT_ITERATOR_TAG);
        setPrototype(prototype);
    }

    @Override
    public String getClassName() {
        return "Iterator";
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
        case Id_next:           arity=0; s="next";           break;
        default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(ELEMENT_ITERATOR_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(ELEMENT_ITERATOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        if (!(thisObj instanceof NativeElementIterator))
            throw incompatibleCallError(f);

        NativeElementIterator iterator = (NativeElementIterator) thisObj;

        switch (id) {
        case Id_next:
            return iterator.next(cx, scope);
        default:
            throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    @Override
    protected int findPrototypeId(String s)
    {
        if (s.equals("next")) {
            return Id_next;
        }
        return 0;
    }

    private Object next(Context cx, Scriptable scope) {
        if (index >= NativeArray.getLengthProperty(cx, arrayLike)) {
            // Out of values. Throw StopIteration.
            throw new JavaScriptException(
                NativeIterator.getStopIterationObject(scope), null, 0);
        }
        return arrayLike.get(index++, arrayLike);
    }

    private Scriptable arrayLike;
    private int index;

    private static final int
        Id_next          = 1,
        MAX_PROTOTYPE_ID = 1;
}
