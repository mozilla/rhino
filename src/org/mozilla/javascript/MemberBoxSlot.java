package org.mozilla.javascript;

/**
 * This is a specialization of Slot to store values that are retrieved via reflection using the
 * MemberBox class.
 */
public class MemberBoxSlot extends Slot {
    MemberBoxSlot(Slot oldSlot) {
        super(oldSlot);
    }

    MemberBox getter;
    MemberBox setter;

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
        String fName = name == null ? "f" : name.toString();
        if (getter != null) {
            desc.defineProperty(
                    "get", getter.asGetterFunction(fName, scope), ScriptableObject.EMPTY);
        }
        if (setter != null) {
            desc.defineProperty(
                    "set", setter.asSetterFunction(fName, scope), ScriptableObject.EMPTY);
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
            Context cx = Context.getContext();
            Class<?>[] pTypes = setter.argTypes;
            // XXX: cache tag since it is already calculated in
            // defineProperty ?
            Class<?> valueType = pTypes[pTypes.length - 1];
            int tag = FunctionObject.getTypeTag(valueType);
            Object actualArg = FunctionObject.convertArg(cx, start, value, tag);

            if (setter.delegateTo == null) {
                setter.invoke(start, new Object[] {actualArg});
            } else {
                setter.invoke(setter.delegateTo, new Object[] {start, actualArg});
            }
            return true;
        }
        return super.setValue(value, owner, start);
    }

    @Override
    public Object getValue(Scriptable start) {
        if (getter != null) {
            if (getter.delegateTo == null) {
                return getter.invoke(start, ScriptRuntime.emptyArgs);
            }
            return getter.invoke(getter.delegateTo, new Object[] {start});
        }
        return super.getValue(start);
    }

    @Override
    Function getSetterFunction(String name, Scriptable scope) {
        if (setter == null) {
            return null;
        }
        return setter.asSetterFunction(name, scope);
    }

    @Override
    Function getGetterFunction(String name, Scriptable scope) {
        if (getter == null) {
            return null;
        }
        return getter.asGetterFunction(name, scope);
    }
}
