/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

import java.util.Objects;

public class IdFunctionObject extends BaseFunction {
    private static final long serialVersionUID = -5332312783643935019L;

    public IdFunctionObject(IdFunctionCall idcall, Object tag, int id, int arity) {
        if (arity < 0) throw new IllegalArgumentException();

        this.idcall = idcall;
        this.tag = tag;
        this.methodId = id;
        this.arity = arity;
    }

    public IdFunctionObject(
            IdFunctionCall idcall, Object tag, int id, String name, int arity, VarScope scope) {
        super(scope, null);

        if (arity < 0) throw new IllegalArgumentException();
        if (name == null) throw new IllegalArgumentException();

        this.idcall = idcall;
        this.tag = tag;
        this.methodId = id;
        this.arity = arity;
        this.functionName = name;
    }

    public void initFunction(String name, VarScope scope) {
        if (name == null) throw new IllegalArgumentException();
        if (scope == null) throw new IllegalArgumentException();
        this.functionName = name;
        setParentScope(scope);
    }

    public final boolean hasTag(Object tag) {
        return Objects.equals(tag, this.tag);
    }

    public Object getTag() {
        return tag;
    }

    public final int methodId() {
        return methodId;
    }

    public final void markAsConstructor(Scriptable prototypeProperty) {
        useCallAsConstructor = true;
        setImmunePrototypeProperty(prototypeProperty);
    }

    public final <T extends PropHolder<T>> void addAsProperty(T target) {
        ScriptableObject.defineProperty(target, functionName, this, ScriptableObject.DONTENUM);
    }

    public void exportAsScopeProperty() {
        addAsProperty(getDeclarationScope());
    }

    @Override
    public Scriptable getPrototype() {
        // Lazy initialization of prototype: for native functions this
        // may not be called at all
        Scriptable proto = super.getPrototype();
        if (proto == null) {
            proto = getFunctionPrototype(getDeclarationScope());
            setPrototype(proto);
        }
        return proto;
    }

    @Override
    public Object call(Context cx, VarScope scope, Object thisObj, Object[] args) {
        // We need to do some sneakiness here for constructors...
        return idcall.execIdCall(this, cx, scope, getThisObj(thisObj), args);
    }

    public final Scriptable getThisObj(Object thisObj) {
        if (useCallAsConstructor && (thisObj == null || Undefined.isUndefined(thisObj))) {
            var res = ScriptableObject.getTopLevelScope(getDeclarationScope()).getGlobalThis();
            return res;
        } else {
            return ScriptRuntime.toObject(getDeclarationScope(), thisObj);
        }
    }

    @Override
    public Scriptable construct(Context cx, VarScope scope, Object[] args) {
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 && this.getHomeObject() != null) {
            // Only methods have home objects associated with them
            throw ScriptRuntime.typeErrorById("msg.not.ctor", getFunctionName());
        }

        Scriptable result = createObject(cx, scope);
        if (result == null) {
            Object val = idcall.execIdCall(this, cx, scope, null, args);
            if (!(val instanceof Scriptable)) {
                // It is program error not to return Scriptable from
                // the call method if createObject returns null.
                throw new IllegalStateException(
                        "Bad implementation of call as constructor, name="
                                + getFunctionName()
                                + " in "
                                + getClass().getName());
            }
            result = (Scriptable) val;
            if (result.getPrototype() == null) {
                Scriptable proto = getClassPrototype();
                if (result != proto) {
                    result.setPrototype(proto);
                }
            }
            if (result.getParentScope() == null) {
                VarScope parent = getParentScope();
                result.setParentScope(parent);
            }
        } else {
            Object val = call(cx, scope, result, args);
            if (val instanceof Scriptable) {
                result = (Scriptable) val;
            }
        }
        return result;
    }

    @Override
    public Scriptable createObject(Context cx, VarScope scope) {
        if (useCallAsConstructor) {
            return null;
        }
        // Throw error if not explicitly coded to be used as constructor,
        // to satisfy ECMAScript standard (see bugzilla 202019).
        // To follow current (2003-05-01) SpiderMonkey behavior, change it to:
        // return super.createObject(cx, scope);
        throw ScriptRuntime.typeErrorById("msg.not.ctor", functionName);
    }

    @Override
    public int getArity() {
        return arity;
    }

    @Override
    public int getLength() {
        return getArity();
    }

    @Override
    public boolean isConstructor() {
        return useCallAsConstructor;
    }

    @Override
    public String getFunctionName() {
        return (functionName == null) ? "" : functionName;
    }

    public final RuntimeException unknown() {
        // It is program error to call id-like methods for unknown function
        return new IllegalArgumentException("BAD FUNCTION ID=" + methodId + " MASTER=" + idcall);
    }

    static boolean equalObjectGraphs(
            IdFunctionObject f1, IdFunctionObject f2, EqualObjectGraphs eq) {
        return f1.methodId == f2.methodId
                && f1.hasTag(f2.tag)
                && eq.equalGraphs(f1.idcall, f2.idcall);
    }

    private final IdFunctionCall idcall;
    private final Object tag;
    private final int methodId;
    private int arity;
    private boolean useCallAsConstructor;
    private String functionName;
}
