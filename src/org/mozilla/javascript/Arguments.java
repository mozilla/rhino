/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Igor Bukanov
 *   Hannes Wallnoefer
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

/**
 * This class implements the "arguments" object.
 *
 * See ECMA 10.1.8
 *
 * @see org.mozilla.javascript.NativeCall
 * @author Norris Boyd
 */
public class Arguments extends ScriptableObject 
{
    static final long serialVersionUID = 4275508002492040609L;

    public Arguments(NativeCall activation) {
        Scriptable parent = activation.getParentScope();
        setParentScope(parent);
        Scriptable scope = ScriptableObject.getTopLevelScope(parent);
        setPrototype(TopLevel.getBuiltinPrototype(scope, TopLevel.Builtins.Object));

        this.activation = activation;
        args = activation.originalArgs;
        constructor = TopLevel.getBuiltinCtor(scope, TopLevel.Builtins.Object);
        lengthObj = Integer.valueOf(args.length);
        calleeObj = activation.function;
    }

    public String getClassName() {
        return "Arguments";
    }

    protected boolean hasArg(int index) {
        return index >= 0
                && index < args.length
                && args[index] != NOT_FOUND;
    }

    // the following helper methods assume that 0 < index < args.length

    protected void setArg(int index, Object value) {
        synchronized (this) {
            if (args == activation.originalArgs) {
                args = args.clone();
            }
            args[index] = value;
        }
        String argName = getActivationName(index);
        if (argName != null) {
            activation.put(argName, activation, value);
        }
    }

    protected Object getArg(int index) {
        String argName = getActivationName(index);
        if (argName != null) {
            return activation.get(argName, activation);
        } else {
            return args[index];
        }
    }

    private void removeArg(int index) {
        if (args[index] != NOT_FOUND) {
            synchronized (this) {
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
        return hasArg(index) || super.has(index, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (hasArg(index)) {
            return getArg(index);
        }
        return super.get(index, start);
    }
    
    /**
     * Return the activation name of the given parameter, or null if the
     * parameter does not have an activation name, either because it is
     * shadowed by a later parameter or is not a defined parameter.
     * @param index the parameter index
     * @return the activation name, or null
     */
    private String getActivationName(int index) {
        NativeFunction f = activation.function;
        int definedCount = f.getParamCount();
        if (index < definedCount) {
            // Check if argument is not hidden by later argument with the same
            // name as hidden arguments are not shared with activation
            String argName = f.getParamOrVarName(index);
            for (int i = index + 1; i < definedCount; i++) {
                if (argName.equals(f.getParamOrVarName(i))) {
                    // argument is shadowed by later argument
                    return null;
                }
            }
            return argName;
        }
        // not a defined parameter
        return null;
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (hasArg(index)) {
            setArg(index, value);
        } else {
            super.put(index, start, value);
        }
    }

    @Override
    public void delete(int index) {
        if (0 <= index && index < args.length) {
            removeArg(index);
        }
        super.delete(index);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (super.has(name, start)) {
            return true;
        } else if (name.equals(LENGTH)) {
            return lengthObj != NOT_FOUND;
        } else if (name.equals(CONSTRUCTOR)) {
            return constructor != NOT_FOUND;
        } else if (name.equals(CALLEE)) {
            return calleeObj != NOT_FOUND;
        }
        return false;
    }

    @Override
    public Object get(String name, Scriptable start) {
        Object value = super.get(name, start);
        if (value == NOT_FOUND) {
            if (name.equals(LENGTH)) {
                value = lengthObj;
            } else if (name.equals(CONSTRUCTOR)) {
                value = constructor;
            } else if (name.equals(CALLEE)) {
                value = calleeObj;
            }
        }
        return value;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        super.put(name, start, value);
        if (name.equals(LENGTH)) {
            lengthObj = NOT_FOUND;
        } else if (name.equals(CONSTRUCTOR)) {
            constructor = NOT_FOUND;
        } else if (name.equals(CALLEE)) {
            calleeObj = NOT_FOUND;
        }
    }

    @Override
    public void delete(String name) {
        super.delete(name);
        if (name.equals(LENGTH)) {
            lengthObj = NOT_FOUND;
        } else if (name.equals(CONSTRUCTOR)) {
            constructor = NOT_FOUND;
        } else if (name.equals(CALLEE)) {
            calleeObj = NOT_FOUND;
        }
    }

    @Override
    Object[] getIds(boolean getAll) {
        int length = args.length;
        Object[] superIds = super.getIds(getAll);
        if (length == 0 && !getAll) {
            return superIds;
        }
        int extraLength = getAll ? length + 3 : length;
        Object[] ownIds = new Object[extraLength];
        int count = 0;
        for (int i = 0; i < length; i++) {
            // avoid adding args which are already present in superIds
            // (or would be if they were enumerable)
            if (hasArg(i) && !super.has(i, this)) {
                ownIds[count++] = Integer.valueOf(i);
            }
        }
        if (getAll) {
            if (lengthObj != NOT_FOUND && !super.has(LENGTH, this))
                ownIds[count++] = LENGTH;
            if (calleeObj != NOT_FOUND && !super.has(CALLEE, this))
                ownIds[count++] = CALLEE;
            if (constructor != NOT_FOUND && !super.has(CONSTRUCTOR, this))
                ownIds[count++] = CONSTRUCTOR;
        }
        if (superIds.length == 0 && count == extraLength) {
            // common case
            return ownIds;
        }
        if (count > 0) {
            int mergedLength = superIds.length + count;
            Object[] merged = new Object[mergedLength];
            System.arraycopy(ownIds, 0, merged, 0, count);
            System.arraycopy(superIds, 0, merged, count, superIds.length);
            return merged;
        }
        return superIds;
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        double d = ScriptRuntime.toNumber(id);
        int index = (int) d;
        if (d != index || !hasArg(index) || super.has(index, this)) {
            ScriptableObject desc = super.getOwnPropertyDescriptor(cx, id);
            if (desc == null) {
                if (id.equals(LENGTH) && lengthObj != NOT_FOUND) {
                    desc = buildDataDescriptor(this, lengthObj, DONTENUM);
                } else if (id.equals(CONSTRUCTOR) && constructor != NOT_FOUND) {
                    desc = buildDataDescriptor(this, constructor, DONTENUM);
                } else if (id.equals(CALLEE) && calleeObj != NOT_FOUND) {
                    desc = buildDataDescriptor(this, calleeObj, DONTENUM);
                }
            }
            return desc;
        }
        return buildDataDescriptor(this, getArg(index), EMPTY);
    }

    @Override
    public void defineOwnProperty(Context cx, Object id, ScriptableObject desc) {
        super.defineOwnProperty(cx, id, desc);

        double d = ScriptRuntime.toNumber(id);
        int index = (int) d;
        if (d != index) return;

        if (!hasArg(index)) return;

        if (isAccessorDescriptor(desc)) {
          removeArg(index);
          return;
        }

        Object newValue = getProperty(desc, "value");

        if (newValue == NOT_FOUND) return;

        setArg(index, newValue);
        if (isFalse(getProperty(desc, "writable"))) {
            removeArg(index);
        }

    }

    private static final String
            CALLEE = "callee",
            CONSTRUCTOR = "constructor",
            LENGTH = "length";
    
    // Fields to hold caller, callee and length properties,
    // where NOT_FOUND value tags deleted properties.
    private Object calleeObj;
    private Object lengthObj;
    private Object constructor;

    private final NativeCall activation;

    // Initially args holds activation.getOriginalArgs(), but any modification
    // of its elements triggers creation of a copy. If its element holds NOT_FOUND,
    // it indicates deleted index, in which case super class is queried.
    private Object[] args;
}
