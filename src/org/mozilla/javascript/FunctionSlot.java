package org.mozilla.javascript;

/**
 * This is a specialization of Slot to store values that are retrieved via calls to script
 * functions.
 */
public class FunctionSlot extends Slot {
    FunctionSlot(Slot oldSlot) {
        super(oldSlot);
    }

    // These must be Object because they could be Undefined
    Object getter;
    Object setter;

    @Override
    boolean isValueSlot() {
        return false;
    }

    @Override
    boolean isSetterSlot() {
        return true;
    }

    @Override
    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        // It sounds logical that this would be the same as the logic for a normal Slot,
        // but the spec is super pedantic about things like the order of properties here,
        // so we need special support here.
        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        desc.setCommonDescriptorProperties(getAttributes(), getter == null && setter == null);

        if (getter != null) {
            desc.defineProperty("get", getter, ScriptableObject.EMPTY);
        }
        if (setter != null) {
            desc.defineProperty("set", setter, ScriptableObject.EMPTY);
        }
        return desc;
    }

    @Override
    public boolean setValue(Object value, Scriptable owner, Scriptable start) {
        if (setter == null) {
            if (getter != null) {
                throwNoSetterException(start, value);
                return true;
            }
        } else {
            if (setter instanceof Function) {
                Function setterFunc = (Function) setter;
                Context cx = Context.getContext();
                setterFunc.call(cx, setterFunc.getParentScope(), start, new Object[] {value});
            }
            return true;
        }
        return super.setValue(value, owner, start);
    }

    @Override
    public Object getValue(Scriptable start) {
        if (getter != null) {
            if (getter instanceof Function) {
                Function getterFunc = (Function) getter;
                Context cx = Context.getContext();
                return getterFunc.call(
                        cx, getterFunc.getParentScope(), start, ScriptRuntime.emptyArgs);
            }
        }
        return this.value;
    }

    @Override
    Function getSetterFunction(String name, Scriptable scope) {
        if (setter instanceof Function) {
            return (Function) setter;
        }
        return null;
    }

    @Override
    Function getGetterFunction(String name, Scriptable scope) {
        if (getter instanceof Function) {
            return (Function) getter;
        }
        return null;
    }
}
