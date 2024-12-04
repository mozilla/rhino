package org.mozilla.javascript;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This is a specialization of property access using some lambda functions. It behaves exactly like
 * any other slot that has only a value, but instead of getting the value directly, it comes from
 * calling the functions. This makes it different from GetterSlot, which lets the user see directly
 * that there is a getter or a setter function involved. This makes this class useful for
 * implementing properties that behave like any other JavaScript property but which are implemented
 * using some native functionality without using reflection.
 */
public class LambdaSlot extends Slot {
    private static final long serialVersionUID = -3046681698806493052L;

    LambdaSlot(Object name, int index) {
        super(name, index, 0);
    }

    LambdaSlot(Slot oldSlot) {
        super(oldSlot);
    }

    @Override
    LambdaSlot copySlot() {
        var newSlot = new LambdaSlot(this);
        newSlot.value = value;
        newSlot.getter = getter;
        newSlot.setter = setter;
        newSlot.next = null;
        newSlot.orderedNext = null;
        return newSlot;
    }

    transient Supplier<Object> getter;
    transient Consumer<Object> setter;

    @Override
    boolean isValueSlot() {
        return false;
    }

    @Override
    boolean isSetterSlot() {
        return false;
    }

    @Override
    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        if (getter != null) {
            desc.defineProperty("value", getter.get(), ScriptableObject.EMPTY);
        } else {
            desc.defineProperty("value", value, ScriptableObject.EMPTY);
        }
        desc.setCommonDescriptorProperties(getAttributes(), true);
        return desc;
    }

    @Override
    public boolean setValue(Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        if (setter != null) {
            if (owner == start) {
                setter.accept(value);
                return true;
            }
            return false;
        }
        return super.setValue(value, owner, start, isThrow);
    }

    @Override
    public Object getValue(Scriptable start) {
        if (getter != null) {
            return getter.get();
        }
        return super.getValue(start);
    }
}
