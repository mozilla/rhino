/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serializable;

/**
 * This class implements the object lookup required for the <code>with</code> statement. It simply
 * delegates every action to its prototype except for operations on its parent.
 */
public class NativeWith implements Scriptable, SymbolScriptable, IdFunctionCall, Serializable {
    private static final long serialVersionUID = 1L;

    static void init(Scriptable scope, boolean sealed) {
        NativeWith obj = new NativeWith();

        obj.setParentScope(scope);
        obj.setPrototype(ScriptableObject.getObjectPrototype(scope));

        IdFunctionObject ctor = new IdFunctionObject(obj, FTAG, Id_constructor, "With", 0, scope);
        ctor.markAsConstructor(obj);
        if (sealed) {
            ctor.sealObject();
        }
        ctor.exportAsScopeProperty();
    }

    private NativeWith() {}

    protected NativeWith(Scriptable parent, Scriptable prototype) {
        this.parent = parent;
        this.prototype = prototype;
    }

    @Override
    public String getClassName() {
        return "With";
    }

    @Override
    public boolean has(String id, Scriptable start) {
        return prototype.has(id, prototype);
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        if (prototype instanceof SymbolScriptable) {
            return ((SymbolScriptable) prototype).has(key, prototype);
        }
        return false;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return prototype.has(index, prototype);
    }

    @Override
    public Object get(String id, Scriptable start) {
        if (start == this) {
            start = prototype;
        }
        return prototype.get(id, start);
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        if (start == this) {
            start = prototype;
        }
        if (prototype instanceof SymbolScriptable) {
            return ((SymbolScriptable) prototype).get(key, start);
        }
        return Scriptable.NOT_FOUND;
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (start == this) {
            start = prototype;
        }
        return prototype.get(index, start);
    }

    @Override
    public void put(String id, Scriptable start, Object value) {
        if (start == this) start = prototype;
        prototype.put(id, start, value);
    }

    @Override
    public void put(Symbol symbol, Scriptable start, Object value) {
        if (start == this) {
            start = prototype;
        }
        if (prototype instanceof SymbolScriptable) {
            ((SymbolScriptable) prototype).put(symbol, start, value);
        }
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (start == this) start = prototype;
        prototype.put(index, start, value);
    }

    @Override
    public void delete(String id) {
        prototype.delete(id);
    }

    @Override
    public void delete(Symbol key) {
        if (prototype instanceof SymbolScriptable) {
            ((SymbolScriptable) prototype).delete(key);
        }
    }

    @Override
    public void delete(int index) {
        prototype.delete(index);
    }

    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    @Override
    public Scriptable getParentScope() {
        return parent;
    }

    @Override
    public void setParentScope(Scriptable parent) {
        this.parent = parent;
    }

    @Override
    public Object[] getIds() {
        return prototype.getIds();
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        return prototype.getDefaultValue(typeHint);
    }

    @Override
    public boolean hasInstance(Scriptable value) {
        return prototype.hasInstance(value);
    }

    /** Must return null to continue looping or the final collection result. */
    protected Object updateDotQuery(boolean value) {
        // NativeWith itself does not support it
        throw new IllegalStateException();
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (f.hasTag(FTAG)) {
            if (f.methodId() == Id_constructor) {
                throw Context.reportRuntimeErrorById("msg.cant.call.indirect", "With");
            }
        }
        throw f.unknown();
    }

    static boolean isWithFunction(Object functionObj) {
        if (functionObj instanceof IdFunctionObject) {
            IdFunctionObject f = (IdFunctionObject) functionObj;
            return f.hasTag(FTAG) && f.methodId() == Id_constructor;
        }
        return false;
    }

    static Object newWithSpecial(Context cx, Scriptable scope, Object[] args) {
        ScriptRuntime.checkDeprecated(cx, "With");
        scope = ScriptableObject.getTopLevelScope(scope);
        NativeWith thisObj = new NativeWith();
        thisObj.setPrototype(
                args.length == 0
                        ? ScriptableObject.getObjectPrototype(scope)
                        : ScriptRuntime.toObject(cx, scope, args[0]));
        thisObj.setParentScope(scope);
        return thisObj;
    }

    private static final Object FTAG = "With";

    private static final int Id_constructor = 1;

    protected Scriptable prototype;
    protected Scriptable parent;
}
