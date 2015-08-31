/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.NativeSymbol.ITERATOR_PROPERTY;
import static org.mozilla.javascript.NativeSymbol.TO_STRING_TAG_PROPERTY;

public abstract class ES6Iterator extends IdScriptableObject {

    static void init(ScriptableObject scope, boolean sealed, IdScriptableObject prototype, String tag) {
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
            scope.associateValue(tag, prototype);
        }
    }

    protected boolean exhausted = false;

    ES6Iterator() {}

    ES6Iterator(Scriptable scope) {
        // Set parent and prototype properties. Since we don't have a
        // "Iterator" constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        IdScriptableObject prototype = (IdScriptableObject)
            ScriptableObject.getTopScopeValue(top, getTag());
        setPrototype(prototype);
    }

    @Override
    protected void initPrototypeId(int id)
    {
        switch (id) {
            case Id_next:
                initPrototypeMethod(getTag(), id, NEXT_METHOD, 0);
                return;
            case Id_iterator:
                initPrototypeMethod(getTag(), id, ITERATOR_PROPERTY, "[Symbol.iterator]", 0);
                return;
            case Id_toStringTag:
                initPrototypeValue(Id_toStringTag, TO_STRING_TAG_PROPERTY, getClassName(), DONTENUM | READONLY);
                return;
            default: throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(getTag())) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        if (!(thisObj instanceof ES6Iterator))
            throw incompatibleCallError(f);

        ES6Iterator iterator = (ES6Iterator) thisObj;

        switch (id) {
        case Id_next:
            return iterator.next(cx, scope);
        case Id_iterator:
            return iterator;
        default:
            throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    @Override
    protected int findPrototypeId(String s) {
        if (s.charAt(0) == 'n') {
            return Id_next;
        } else if (ITERATOR_PROPERTY.equals(s)) {
            return Id_iterator;
        } else if (TO_STRING_TAG_PROPERTY.equals(s)) {
            return Id_toStringTag;
        }
        return 0;
    }

    abstract protected boolean isDone(Context cx, Scriptable scope);

    abstract protected Object nextValue(Context cx, Scriptable scope);

    protected Object next(Context cx, Scriptable scope) {
        Object value = Undefined.instance;
        boolean done = isDone(cx, scope) || this.exhausted;
        if (!done) {
            value = nextValue(cx, scope);
        } else {
            this.exhausted = true;
        }
        return makeIteratorResult(cx, scope, done, value);
    }

    abstract protected String getTag();

    // 25.1.1.3 The IteratorResult Interface
    private Scriptable makeIteratorResult(Context cx, Scriptable scope, boolean done, Object value) {
        Scriptable iteratorResult = cx.newObject(scope);
        ScriptableObject.putProperty(iteratorResult, VALUE_PROPERTY, value);
        ScriptableObject.putProperty(iteratorResult, DONE_PROPERTY, done);
        return iteratorResult;
    }

    private static final int
        Id_next             = 1,
        Id_iterator         = 2,
        Id_toStringTag      = 3,
        MAX_PROTOTYPE_ID    = Id_toStringTag;

    public static final String NEXT_METHOD = "next";
    public static final String DONE_PROPERTY = "done";
    public static final String VALUE_PROPERTY = "value";
}
