/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the "arguments" object.
 *
 * <p>See ECMA 10.1.8
 *
 * @see org.mozilla.javascript.NativeCall
 * @author Norris Boyd
 */
final class Arguments extends IdScriptableObject {
    private static final long serialVersionUID = 4275508002492040609L;

    private static final String FTAG = "Arguments";

    public Arguments(NativeCall activation) {
        this.activation = activation;

        Scriptable parent = activation.getParentScope();
        setParentScope(parent);
        setPrototype(ScriptableObject.getObjectPrototype(parent));

        args = activation.originalArgs;
        lengthObj = Integer.valueOf(args.length);

        NativeFunction f = activation.function;
        calleeObj = f;

        int version = f.getLanguageVersion();
        if (version <= Context.VERSION_1_3 && version != Context.VERSION_DEFAULT) {
            callerObj = null;
        } else {
            callerObj = NOT_FOUND;
        }

        defineProperty(
                SymbolKey.ITERATOR,
                TopLevel.getBuiltinPrototype(
                                ScriptableObject.getTopLevelScope(parent), TopLevel.Builtins.Array)
                        .get("values", parent),
                ScriptableObject.DONTENUM);
    }

    @Override
    public String getClassName() {
        return FTAG;
    }

    private Object arg(int index) {
        if (index < 0 || args.length <= index) return NOT_FOUND;
        return args[index];
    }

    // the following helper methods assume that 0 < index < args.length

    private void putIntoActivation(int index, Object value) {
        String argName = activation.function.getParamOrVarName(index);
        activation.put(argName, activation, value);
    }

    private Object getFromActivation(int index) {
        String argName = activation.function.getParamOrVarName(index);
        return activation.get(argName, activation);
    }

    private void replaceArg(int index, Object value) {
        if (sharedWithActivation(index)) {
            putIntoActivation(index, value);
        }
        synchronized (this) {
            if (args == activation.originalArgs) {
                args = args.clone();
            }
            args[index] = value;
        }
    }

    private void removeArg(int index) {
        synchronized (this) {
            if (args[index] != NOT_FOUND) {
                if (args == activation.originalArgs) {
                    args = args.clone();
                }
                args[index] = NOT_FOUND;
            }
        }
    }

    // end helpers

    @Override
    public boolean has(int index, Scriptable start) {
        if (arg(index) != NOT_FOUND) {
            return true;
        }
        return super.has(index, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        final Object value = arg(index);
        if (value == NOT_FOUND) {
            return super.get(index, start);
        }
        if (sharedWithActivation(index)) {
            return getFromActivation(index);
        }
        return value;
    }

    private boolean sharedWithActivation(int index) {
        Context cx = Context.getContext();
        if (cx.isStrictMode()) {
            return false;
        }
        NativeFunction f = activation.function;

        // Check if default arguments are present
        if (f == null || f.hasDefaultParameters()) {
            return false;
        }

        int definedCount = f.getParamCount();
        if (index < definedCount) {
            // Check if argument is not hidden by later argument with the same
            // name as hidden arguments are not shared with activation
            if (index < definedCount - 1) {
                String argName = f.getParamOrVarName(index);
                for (int i = index + 1; i < definedCount; i++) {
                    if (argName.equals(f.getParamOrVarName(i))) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (arg(index) == NOT_FOUND) {
            super.put(index, start, value);
        } else {
            replaceArg(index, value);
        }
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        super.put(name, start, value);
    }

    @Override
    public void delete(int index) {
        if (0 <= index && index < args.length) {
            removeArg(index);
        }
        super.delete(index);
    }

    private static final int Id_callee = 1,
            Id_length = 2,
            Id_caller = 3,
            MAX_INSTANCE_ID = Id_caller;

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
        switch (s) {
            case "callee":
                id = Id_callee;
                break;
            case "length":
                id = Id_length;
                break;
            case "caller":
                id = Id_caller;
                break;
            default:
                id = 0;
                break;
        }
        Context cx = Context.getContext();
        if (cx.isStrictMode()) {
            if (id == Id_callee || id == Id_caller) {
                return super.findInstanceIdInfo(s);
            }
        }

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
            case Id_callee:
                attr = calleeAttr;
                break;
            case Id_caller:
                attr = callerAttr;
                break;
            case Id_length:
                attr = lengthAttr;
                break;
            default:
                throw new IllegalStateException();
        }
        return instanceIdInfo(attr, id);
    }

    @Override
    protected String getInstanceIdName(int id) {
        switch (id) {
            case Id_callee:
                return "callee";
            case Id_length:
                return "length";
            case Id_caller:
                return "caller";
        }
        return null;
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        switch (id) {
            case Id_callee:
                return calleeObj;
            case Id_length:
                return lengthObj;
            case Id_caller:
                {
                    Object value = callerObj;
                    if (value == UniqueTag.NULL_VALUE) {
                        value = null;
                    } else if (value == null) {
                        NativeCall caller = activation.parentActivationCall;
                        if (caller != null) {
                            value = caller.get("arguments", caller);
                        }
                    }
                    return value;
                }
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value) {
        switch (id) {
            case Id_callee:
                calleeObj = value;
                return;
            case Id_length:
                lengthObj = value;
                return;
            case Id_caller:
                callerObj = (value != null) ? value : UniqueTag.NULL_VALUE;
                return;
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr) {
        switch (id) {
            case Id_callee:
                calleeAttr = attr;
                return;
            case Id_length:
                lengthAttr = attr;
                return;
            case Id_caller:
                callerAttr = attr;
                return;
        }
        super.setInstanceIdAttributes(id, attr);
    }

    @Override
    Object[] getIds(boolean getNonEnumerable, boolean getSymbols) {
        Object[] ids = super.getIds(getNonEnumerable, getSymbols);
        if (args.length != 0) {
            boolean[] present = new boolean[args.length];
            int extraCount = args.length;
            for (int i = 0; i != ids.length; ++i) {
                Object id = ids[i];
                if (id instanceof Integer) {
                    int index = ((Integer) id).intValue();
                    if (0 <= index && index < args.length) {
                        if (!present[index]) {
                            present[index] = true;
                            extraCount--;
                        }
                    }
                }
            }
            if (!getNonEnumerable) { // avoid adding args which were redefined to non-enumerable
                for (int i = 0; i < present.length; i++) {
                    if (!present[i] && super.has(i, this)) {
                        present[i] = true;
                        extraCount--;
                    }
                }
            }
            if (extraCount != 0) {
                Object[] tmp = new Object[extraCount + ids.length];
                System.arraycopy(ids, 0, tmp, extraCount, ids.length);
                ids = tmp;
                int offset = 0;
                for (int i = 0; i != args.length; ++i) {
                    if (!present[i]) {
                        ids[offset] = Integer.valueOf(i);
                        ++offset;
                    }
                }
                if (offset != extraCount) Kit.codeBug();
            }
        }
        return ids;
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        if (ScriptRuntime.isSymbol(id) || id instanceof Scriptable) {
            return super.getOwnPropertyDescriptor(cx, id);
        }

        double d = ScriptRuntime.toNumber(id);
        int index = (int) d;
        if (d != index) {
            return super.getOwnPropertyDescriptor(cx, id);
        }
        Object value = arg(index);
        if (value == NOT_FOUND) {
            return super.getOwnPropertyDescriptor(cx, id);
        }
        if (sharedWithActivation(index)) {
            value = getFromActivation(index);
        }
        if (super.has(index, this)) { // the descriptor has been redefined
            ScriptableObject desc = super.getOwnPropertyDescriptor(cx, id);
            desc.put("value", desc, value);
            return desc;
        }
        Scriptable scope = getParentScope();
        if (scope == null) scope = this;
        return buildDataDescriptor(scope, value, EMPTY);
    }

    @Override
    protected boolean defineOwnProperty(
            Context cx, Object id, ScriptableObject desc, boolean checkValid) {
        super.defineOwnProperty(cx, id, desc, checkValid);
        if (ScriptRuntime.isSymbol(id)) {
            return true;
        }

        double d = ScriptRuntime.toNumber(id);
        int index = (int) d;
        if (d != index) return true;

        Object value = arg(index);
        if (value == NOT_FOUND) return true;

        if (isAccessorDescriptor(desc)) {
            removeArg(index);
            return true;
        }

        Object newValue = getProperty(desc, "value");
        if (newValue == NOT_FOUND) return true;

        replaceArg(index, newValue);

        if (isFalse(getProperty(desc, "writable"))) {
            removeArg(index);
        }
        return true;
    }

    // ECMAScript2015
    // 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
    //   8. Perform DefinePropertyOrThrow(obj, "caller", PropertyDescriptor {[[Get]]:
    // %ThrowTypeError%,
    //      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
    //   9. Perform DefinePropertyOrThrow(obj, "callee", PropertyDescriptor {[[Get]]:
    // %ThrowTypeError%,
    //      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
    void defineAttributesForStrictMode() {
        Context cx = Context.getContext();
        if (!cx.isStrictMode()) {
            return;
        }
        setGetterOrSetter("caller", 0, new ThrowTypeError("caller"), true);
        setGetterOrSetter("caller", 0, new ThrowTypeError("caller"), false);
        setGetterOrSetter("callee", 0, new ThrowTypeError("callee"), true);
        setGetterOrSetter("callee", 0, new ThrowTypeError("callee"), false);
        setAttributes("caller", DONTENUM | PERMANENT);
        setAttributes("callee", DONTENUM | PERMANENT);
        callerObj = null;
        calleeObj = null;
    }

    private static class ThrowTypeError extends BaseFunction {
        private static final long serialVersionUID = -744615873947395749L;
        private String propertyName;

        ThrowTypeError(String propertyName) {
            this.propertyName = propertyName;
            super.setInstanceIdAttributes(BaseFunction.Id_name, PERMANENT | READONLY | DONTENUM);
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            throw ScriptRuntime.typeErrorById("msg.arguments.not.access.strict", propertyName);
        }
    }

    // Fields to hold caller, callee and length properties,
    // where NOT_FOUND value tags deleted properties.
    // In addition if callerObj == NULL_VALUE, it tags null for scripts, as
    // initial callerObj == null means access to caller arguments available
    // only in JS <= 1.3 scripts
    private Object callerObj;
    private Object calleeObj;
    private Object lengthObj;

    private int callerAttr = DONTENUM;
    private int calleeAttr = DONTENUM;
    private int lengthAttr = DONTENUM;

    private NativeCall activation;

    // Initially args holds activation.getOriginalArgs(), but any modification
    // of its elements triggers creation of a copy. If its element holds NOT_FOUND,
    // it indicates deleted index, in which case super class is queried.
    private Object[] args;
}
