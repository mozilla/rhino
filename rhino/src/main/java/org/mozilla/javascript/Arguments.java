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
class Arguments extends ScriptableObject {
    private static final long serialVersionUID = 4275508002492040609L;

    private static final String CLASS_NAME = "Arguments";

    // Fields to hold caller, callee and length properties,
    // where NOT_FOUND value tags deleted properties.
    // In addition, the 'caller' arguments is only available in JS <= 1.3 scripts
    private Object calleeObj;
    private Object lengthObj;

    private NativeCall activation;

    // Initially args holds activation.getOriginalArgs(), but any modification
    // of its elements triggers creation of a copy. If its element holds NOT_FOUND,
    // it indicates deleted index, in which case super class is queried.
    private Object[] args;

    public Arguments(NativeCall activation, Context cx) {
        this.activation = activation;

        Scriptable parent = activation.getParentScope();
        setParentScope(parent);
        setPrototype(ScriptableObject.getObjectPrototype(parent));

        args = activation.originalArgs;
        lengthObj = Integer.valueOf(args.length);

        JSFunction f = activation.function;
        calleeObj = f;

        defineProperty(
                SymbolKey.ITERATOR,
                TopLevel.getBuiltinPrototype(
                                ScriptableObject.getTopLevelScope(parent), TopLevel.Builtins.Array)
                        .get("values", parent),
                ScriptableObject.DONTENUM);
        defineProperty("length", lengthObj, ScriptableObject.DONTENUM);

        if (activation.isStrict) {
            // ECMAScript2015
            // 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
            //   8. Perform DefinePropertyOrThrow(obj, "caller", PropertyDescriptor {[[Get]]:
            // %ThrowTypeError%,
            //      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
            //   9. Perform DefinePropertyOrThrow(obj, "callee", PropertyDescriptor {[[Get]]:
            // %ThrowTypeError%,
            //      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
            BaseFunction typeErrorThrower = ScriptRuntime.typeErrorThrower(cx);
            int version = cx.getLanguageVersion();
            if (version <= Context.VERSION_1_8) {
                setGetterOrSetter("caller", 0, typeErrorThrower, true);
                setGetterOrSetter("caller", 0, typeErrorThrower, false);
                setGetterOrSetter("callee", 0, typeErrorThrower, true);
                setGetterOrSetter("callee", 0, typeErrorThrower, false);
                setAttributes("caller", DONTENUM | PERMANENT);
                setAttributes("callee", DONTENUM | PERMANENT);
            } else {
                setGetterOrSetter("callee", 0, typeErrorThrower, true);
                setGetterOrSetter("callee", 0, typeErrorThrower, false);
                setAttributes("callee", DONTENUM | PERMANENT);
            }
            calleeObj = null;
        } else {
            defineProperty("callee", calleeObj, ScriptableObject.DONTENUM);

            int version = cx.getLanguageVersion();
            if (version <= Context.VERSION_1_3 && version != Context.VERSION_DEFAULT) {
                defineProperty("caller", (Object) null, ScriptableObject.DONTENUM);
            }
        }
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
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
        JSFunction f = activation.function;

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
    public void put(Symbol key, Scriptable start, Object value) {
        super.put(key, start, value);
    }

    @Override
    public void delete(int index) {
        if (0 <= index && index < args.length) {
            removeArg(index);
        }
        super.delete(index);
    }

    @Override
    Object[] getIds(CompoundOperationMap map, boolean getNonEnumerable, boolean getSymbols) {
        Object[] ids = super.getIds(map, getNonEnumerable, getSymbols);
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
    protected DescriptorInfo getOwnPropertyDescriptor(Context cx, Object id) {
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
            DescriptorInfo desc = super.getOwnPropertyDescriptor(cx, id);
            desc.value = value;
            return desc;
        }
        return buildDataDescriptor(value, EMPTY);
    }

    @Override
    protected boolean defineOwnProperty(
            Context cx, Object id, DescriptorInfo desc, boolean checkValid) {
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

        Object newValue = desc.value;
        if (newValue == NOT_FOUND) return true;

        replaceArg(index, newValue);

        if (isFalse(desc.writable)) {
            removeArg(index);
        }
        return true;
    }

    static final class ReadonlyArguments extends Arguments {
        private boolean initialized;

        public ReadonlyArguments(Arguments arguments, Context cx) {
            super(arguments.activation, cx);
            initialized = true;
        }

        @Override
        public void put(int index, Scriptable start, Object value) {
            if (initialized) {
                return;
            }
            super.put(index, start, value);
        }

        @Override
        public void put(String name, Scriptable start, Object value) {
            if (initialized) {
                return;
            }
            super.put(name, start, value);
        }

        @Override
        public void put(Symbol key, Scriptable start, Object value) {
            if (initialized) {
                return;
            }
            super.put(key, start, value);
        }

        @Override
        public void delete(int index) {
            if (initialized) {
                return;
            }
            super.delete(index);
        }

        @Override
        public void delete(String name) {
            if (initialized) {
                return;
            }
            super.delete(name);
        }

        @Override
        public void delete(Symbol key) {
            if (initialized) {
                return;
            }
            super.delete(key);
        }
    }
}
