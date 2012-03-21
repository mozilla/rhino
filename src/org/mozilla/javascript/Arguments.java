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
final class Arguments extends IdScriptableObject
{
    static final long serialVersionUID = 4275508002492040609L;

    private static final String FTAG = "Arguments";

    public Arguments(NativeCall activation, boolean strict)
    {
        this.activation = activation;
        this.strict = strict;

        Scriptable parent = activation.getParentScope();
        setParentScope(parent);
        setPrototype(ScriptableObject.getObjectPrototype(parent));

        args = activation.originalArgs;
        lengthObj = Integer.valueOf(args.length);

        NativeFunction f = activation.function;
        calleeObj = f;

        int version = f.getLanguageVersion();
        if (version <= Context.VERSION_1_3
            && version != Context.VERSION_DEFAULT)
        {
            callerObj = null;
        } else {
            callerObj = NOT_FOUND;
        }
    }

    @Override
    public String getClassName()
    {
        return FTAG;
    }

    private Object arg(int index) {
      if (index < 0 || args.length <= index) return NOT_FOUND;
      return args[index];
    }

    // the following helper methods assume that 0 < index < args.length

    private void putIntoActivation(int index, Object value) {
        String argName = activation.function.getParamOrVarName(index);
        // checked = false because we're never in strict-mode for this call
        activation.put(argName, activation, value, false);
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
    public boolean has(String name, Scriptable start) {
        if (strict) {
            // must place check here instead of getInstanceIdValue()
            if ("callee".equals(name) || "caller".equals(name)) {
                return true;
            }
        }
        return super.has(name, start);
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (strict) {
            // must place check here instead of getInstanceIdValue()
            if ("callee".equals(name) || "caller".equals(name)) {
                throw ScriptRuntime.typeError0("msg.op.not.allowed");
            }
        }
        return super.get(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value, boolean checked) {
        if (strict) {
            // must place check here instead of setInstanceIdValue()
            if ("callee".equals(name) || "caller".equals(name)) {
                throw ScriptRuntime.typeError0("msg.op.not.allowed");
            }
        }
        super.put(name, start, value, checked);
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        if (arg(index) != NOT_FOUND) {
          return true;
        }
        return super.has(index, start);
    }

    @Override
    public Object get(int index, Scriptable start)
    {
      final Object value = arg(index);
      if (value == NOT_FOUND) {
        return super.get(index, start);
      } else {
        if (sharedWithActivation(index)) {
          return getFromActivation(index);
        } else {
          return value;
        }
      }
    }

    private boolean sharedWithActivation(int index)
    {
        if (strict) {
            // arguments are not shared with activation in strict mode
            return false;
        }
        NativeFunction f = activation.function;
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
    public void put(int index, Scriptable start, Object value, boolean checked)
    {
        if (arg(index) == NOT_FOUND) {
          super.put(index, start, value, checked);
        } else {
          replaceArg(index, value);
        }
    }

    @Override
    public void delete(int index, boolean checked)
    {
        if (0 <= index && index < args.length) {
          removeArg(index);
        }
        super.delete(index, checked);
    }

// #string_id_map#

    private static final int
        Id_callee           = 1,
        Id_length           = 2,
        Id_caller           = 3,

        MAX_INSTANCE_ID     = Id_caller;

    @Override
    protected int getMaxInstanceId()
    {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s)
    {
        int id;
// #generated# Last update: 2010-01-06 05:48:21 ARST
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==6) {
                c=s.charAt(5);
                if (c=='e') { X="callee";id=Id_callee; }
                else if (c=='h') { X="length";id=Id_length; }
                else if (c=='r') { X="caller";id=Id_caller; }
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#

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
          default: throw new IllegalStateException();
        }
        return instanceIdInfo(attr, id);
    }

// #/string_id_map#

    @Override
    protected String getInstanceIdName(int id)
    {
        switch (id) {
            case Id_callee: return "callee";
            case Id_length: return "length";
            case Id_caller: return "caller";
        }
        return null;
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        switch (id) {
            case Id_callee: return calleeObj;
            case Id_length: return lengthObj;
            case Id_caller: {
                Object value = callerObj;
                if (value == UniqueTag.NULL_VALUE) { value = null; }
                else if (value == null) {
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
    protected void setInstanceIdValue(int id, Object value)
    {
        switch (id) {
            case Id_callee: calleeObj = value; return;
            case Id_length: lengthObj = value; return;
            case Id_caller:
                callerObj = (value != null) ? value : UniqueTag.NULL_VALUE;
                return;
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr) {
        switch (id) {
            case Id_callee: calleeAttr = attr; return;
            case Id_length: lengthAttr = attr; return;
            case Id_caller: callerAttr = attr; return;
        }
        super.setInstanceIdAttributes(id, attr);
    }

    @Override
    Object[] getIds(boolean getAll)
    {
        Object[] ids = super.getIds(getAll);
        if (args.length != 0) {
            boolean[] present = new boolean[args.length];
            int extraCount = args.length;
            for (int i = 0; i != ids.length; ++i) {
                Object id = ids[i];
                if (id instanceof Integer) {
                    int index = ((Integer)id).intValue();
                    if (0 <= index && index < args.length) {
                        if (!present[index]) {
                            present[index] = true;
                            extraCount--;
                        }
                    }
                }
            }
            if (!getAll) { // avoid adding args which were redefined to non-enumerable
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
                    if (present == null || !present[i]) {
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
    protected boolean defineOwnProperty(String name, PropertyDescriptor desc,
            boolean checked) {
        double d = ScriptRuntime.toNumber(name);
        int index = (int) d;
        boolean isMapped = (d == index && arg(index) != NOT_FOUND);

        boolean allowed = super.defineOwnProperty(name, desc, checked);
        if (!allowed) {
            return false;
        }

        if (isMapped) {
            if (desc.isAccessorDescriptor()) {
                removeArg(index);
            } else {
                if (desc.hasValue()) {
                    replaceArg(index, desc.getValue());
                }
                if (desc.hasWritable() && !desc.isWritable()) {
                    removeArg(index);
                }
            }
        }
        return true;
    }

    @Override
    protected PropertyDescriptor getOwnProperty(String name) {
        if (strict) {
            if ("callee".equals(name) || "caller".equals(name)) {
                Function thrower = ScriptRuntime.typeErrorThrower();
                return new PropertyDescriptor(thrower, thrower, DONTENUM | PERMANENT);
            }
        }
        PropertyDescriptor desc = super.getOwnProperty(name);
        double d = ScriptRuntime.toNumber(name);
        int index = (int) d;
        if (d != index) {
            return desc;
        }
        Object value = arg(index);
        if (value == NOT_FOUND) {
            return desc;
        }
        if (sharedWithActivation(index)) {
            value = getFromActivation(index);
        }
        if (desc != null) { // the descriptor has been redefined
            desc.setValue(value);
        } else {
            desc = new PropertyDescriptor(value, EMPTY);
        }
        return desc;
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

    private final NativeCall activation;
    private final boolean strict;

// Initially args holds activation.getOriginalArgs(), but any modification
// of its elements triggers creation of a copy. If its element holds NOT_FOUND,
// it indicates deleted index, in which case super class is queried.
    private Object[] args;
}
