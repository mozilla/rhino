/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;

public abstract class ES6Iterator extends ScriptableObject {

    private static final long serialVersionUID = 2438373029140003950L;

    public static final String NEXT_METHOD = "next";
    public static final String DONE_PROPERTY = "done";
    public static final String RETURN_PROPERTY = "return";
    public static final String VALUE_PROPERTY = "value";
    public static final String RETURN_METHOD = "return";

    public static ClassDescriptor makeDescriptor(String name, String tag) {
        // Need a way to associate the built object with the tag. We
        // kind of need this in general, and at the top level, but we
        // can bodge it for now.
        return new ClassDescriptor.Builder(name)
                .withMethod(CTOR, "next", 0, ES6Iterator::js_next)
                .withMethod(CTOR, SymbolKey.ITERATOR, 1, ES6Iterator::js_iterator)
                .withProp(CTOR, SymbolKey.TO_STRING_TAG, value(tag, DONTENUM | READONLY))
                .build();
    }

    public static void initialize(
            ClassDescriptor desc,
            Context cx,
            TopLevel scope,
            ScriptableObject obj,
            boolean sealed,
            String name) {
        var global = desc.populateGlobal(cx, scope, obj, sealed);
        scope.associateValue(name, global);
    }

    protected boolean exhausted = false;
    private String tag;

    protected ES6Iterator() {}

    protected ES6Iterator(VarScope scope, String tag) {
        // Set parent and prototype properties. Since we don't have a
        // "Iterator" constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        this.tag = tag;
        TopLevel top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        ScriptableObject prototype = (ScriptableObject) ScriptableObject.getTopScopeValue(top, tag);
        setPrototype(prototype);
    }

    private static ES6Iterator realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, ES6Iterator.class);
    }

    private static Object js_next(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ES6Iterator iterator = realThis(thisObj);
        return iterator.next(cx, s);
    }

    private static Object js_iterator(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return thisObj;
    }

    protected abstract boolean isDone(Context cx, VarScope scope);

    protected abstract Object nextValue(Context cx, VarScope scope);

    protected Object next(Context cx, VarScope scope) {
        Object value = Undefined.instance;
        boolean done = isDone(cx, scope) || this.exhausted;
        if (!done) {
            value = nextValue(cx, scope);
        } else {
            this.exhausted = true;
        }
        return makeIteratorResult(cx, scope, Boolean.valueOf(done), value);
    }

    protected String getTag() {
        return tag;
    }

    // 25.1.1.3 The IteratorResult Interface
    static Scriptable makeIteratorResult(Context cx, VarScope scope, Boolean done) {
        return makeIteratorResult(cx, scope, done, Undefined.instance);
    }

    static Scriptable makeIteratorResult(Context cx, VarScope scope, Boolean done, Object value) {
        final Scriptable iteratorResult = cx.newObject(scope);
        ScriptableObject.putProperty(iteratorResult, VALUE_PROPERTY, value);
        ScriptableObject.putProperty(iteratorResult, DONE_PROPERTY, done);
        return iteratorResult;
    }
}
