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

 package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Arguments;
import org.mozilla.javascript.NativeCall;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;

import java.util.Arrays;

/**
 * <p>A function activation object that eliminates hash lookups for parameter
 * and local variable access. This works in conjunction with {@link Codegen}
 * resolving the depth and index of bound names at compile time, and
 * {@link OptRuntime} providing optimized methods to access these names.</p>
 *
 * <p>{@code OptCall} optimization is used when a function requires activation
 * (for instance because there are nested functions) but does not contain calls
 * to {@code eval()}, since {@code eval()} may allocate variables in the local
 * function scope that are not known at compile time.</p>
 *
 * <p>Although this class implements some some of the methods defined in the
 * {@link Scriptable} interface, the only case in which those should be used
 * is for interpreted {@code eval()} code in nested functions.</p>
 *
 * @author Hannes Wallnoefer
 */
public class OptCall extends NativeCall {

    private Object[] locals;       // local variables
    private Object[] args;         // copy-on-write args

    private int paramAndVarCount;  // number of declared params + local vars
    private int localsStart;       // index of first local (var or missing arg)
    private transient Object arguments;   // the arguments object
    private transient ObjToIntMap propertyMap;

    public final static int ARGUMENTS_ID = Integer.MAX_VALUE;
    private static final long serialVersionUID = 2569902684936819858L;

    public OptCall(NativeFunction function, Scriptable scope, Object[] args) {
        super(function, args);
        this.args = args;
        setParentScope(scope);
        // leave prototype null

        int paramCount = function.getParamCount();
        this.paramAndVarCount = function.getParamAndVarCount();

        // Declared arguments not provided by the caller are treated as locals,
        // i.e. they can be accessed by name but not through the arguments object
        this.localsStart = Math.min(args.length, paramCount);
        int localsLength = paramAndVarCount - localsStart;
        if (localsLength > 0) {
            locals = new Object[localsLength];
            Arrays.fill(locals, 0, localsLength, Undefined.instance);
        }
    }

    /**
     * Get the value of a parameter or local variable in this activation object.
     * @param index the parameter or variable index
     * @return the
     */
    public Object get(int index) {
        if (index >= 0 && index < localsStart) {
            return args[index];
        } else if (index >= localsStart && index < paramAndVarCount) {
            Object value = locals[index - localsStart];
            return value instanceof ConstHolder ?
                    ((ConstHolder)value).value : value;
        } else if (index == ARGUMENTS_ID) {
            return getArguments();
        } else {
            return Undefined.instance;
        }
    }

    /**
     * Set a parameter or local variable in this activation object.
     * @param index the parameter or variable index
     * @param value the value
     */
    public void set(int index, Object value) {
        if (index >= 0 && index < localsStart) {
            setArg(index, value);
        } else if (index >= localsStart && index < paramAndVarCount) {
            int i = index - localsStart;
            if (!(locals[i] instanceof ConstHolder)) {
                locals[i] = value;
            }
        } else if (index == ARGUMENTS_ID) {
            arguments = value == null ? UniqueTag.NULL_VALUE : value;
        }
    }

    protected void setArg(int index, Object value) {
        if (args == originalArgs) {
            synchronized (this) {
                if (args == originalArgs) {
                    args = args.clone();
                }
            }
        }
        args[index] = value;
    }

    /**
     * Set a constant in this activation object.
     * @param index the constant index
     * @param value the constant value
     */
    public void setConst(int index, Object value) {
        assert (index >= localsStart && index < paramAndVarCount);
        assert (locals[index - localsStart] == Undefined.instance);

        locals[index - localsStart] = new ConstHolder(value);
    }

    public Object getArguments() {
        if (arguments == UniqueTag.NULL_VALUE) {
            return null;
        }
        if (arguments == null) {
            synchronized (this) {
                if (arguments == null)
                    arguments = new OptArguments();
            }
        }
        return arguments;
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (propertyMap == null) initPropertyMap();
        int index = propertyMap.get(name, -1);
        if (index > -1) {
            return get(index);
        }
        return super.get(name, start);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (propertyMap == null) initPropertyMap();
        return propertyMap.has(name) || super.has(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (propertyMap == null) initPropertyMap();
        int index = propertyMap.get(name, -1);
        if (index > -1) {
            set(index, value);
        } else {
            super.put(name, start, value);
        }
    }

    private synchronized void initPropertyMap() {
        // Create a name->index map that allows us to implement methods of
        // the Scriptable interface. Note that these are only for interpreted
        // eval() code and should never be called by optimized code.
        if (propertyMap == null) {
            ObjToIntMap map = new ObjToIntMap(paramAndVarCount);
            int paramCount = function.getParamCount();
            for (int i = 0; i < paramCount; i++) {
                map.put(function.getParamOrVarName(i), i);
            }
            // Only add arguments object if it isn't shadowed by a parameter
            // with the same name
            if (!map.has("arguments")) {
                map.put("arguments", ARGUMENTS_ID);
            }
            for (int i = paramCount; i < paramAndVarCount; i++) {
                String name = function.getParamOrVarName(i);
                if (!map.has(name)) {
                    map.put(function.getParamOrVarName(i), i);
                }
            }
            propertyMap = map;
        }
    }

    
    public class OptArguments extends Arguments {

        private static final long serialVersionUID = -3105597347316602232L;

        OptArguments() {
            super(OptCall.this);
        }

        @Override
        protected Object getArg(int index) {
            return args[index];
        }

        @Override
        protected void setArg(int index, Object value) {
            OptCall.this.setArg(index, value);
        }
    }
    
}

class ConstHolder {
    final Object value;

    ConstHolder(Object value) {
        this.value = value;
    }
}
