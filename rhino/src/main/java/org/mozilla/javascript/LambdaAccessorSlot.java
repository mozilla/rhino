package org.mozilla.javascript;

import org.mozilla.javascript.ScriptableObject.DescriptorInfo;

/**
 * A specialized property accessor using lambda functions, similar to {@link LambdaSlot}, but allows
 * defining properties with getter and setter lambdas that require access to the owner object
 * ('this'). This enables the implementation of properties that can access instance fields of the
 * owner.
 *
 * <p>Unlike {@link LambdaSlot}, Lambda functions used to define getter and setter logic require the
 * owner's `Scriptable` object as one of the parameters. This is particularly useful for
 * implementing properties that behave like standard JavaScript properties, but are implemented with
 * native functionality without the need for reflection.
 */
public class LambdaAccessorSlot extends Slot {
    private ScriptableObject.LambdaGetterFunction getter;
    private ScriptableObject.LambdaSetterFunction setter;
    private LambdaFunction getterFunction;
    private LambdaFunction setterFunction;

    LambdaAccessorSlot(Object name, int index) {
        super(name, index, 0);
    }

    LambdaAccessorSlot(Slot oldSlot) {
        super(oldSlot);
    }

    AccessorSlot asAccessorSlot() {
        AccessorSlot accessorSlot = new AccessorSlot(this);
        if (getterFunction != null) {
            accessorSlot.getter = new AccessorSlot.FunctionGetter(getterFunction);
        }
        if (setterFunction != null) {
            accessorSlot.setter = new AccessorSlot.FunctionSetter(setterFunction);
        }
        return accessorSlot;
    }

    @Override
    LambdaAccessorSlot copySlot() {
        var newSlot = new LambdaAccessorSlot(this);
        newSlot.value = value;
        newSlot.getter = getter;
        newSlot.setter = setter;
        newSlot.getterFunction = getterFunction;
        newSlot.setterFunction = setterFunction;
        newSlot.next = null;
        newSlot.orderedNext = null;
        return newSlot;
    }

    @Override
    boolean isValueSlot() {
        return false;
    }

    @Override
    boolean isSetterSlot() {
        return true;
    }

    @Override
    DescriptorInfo getPropertyDescriptor(Context cx, Scriptable scope) {
        return buildPropertyDescriptor(cx);
    }

    /**
     * The method exists avoid changing the getPropertyDescriptor signature and at the same time to
     * make it explicit that we don't use Scriptable scope parameter of getPropertyDescriptor, since
     * it can be problematic when called from inside ThreadSafeSlotMapContainer::compute lambda
     * which can lead to deadlocks.
     */
    public DescriptorInfo buildPropertyDescriptor(Context cx) {
        int attr = getAttributes();
        DescriptorInfo desc;
        boolean es6 = cx.getLanguageVersion() >= Context.VERSION_ES6;
        if (es6) {
            desc = new DescriptorInfo(ScriptableObject.NOT_FOUND, attr, false);
            if (getterFunction == null && setterFunction == null) {
                desc.writable = (attr & ScriptableObject.READONLY) == 0;
            }
        } else {
            desc =
                    new DescriptorInfo(
                            ScriptableObject.NOT_FOUND,
                            attr,
                            getterFunction == null && setterFunction == null);
        }

        if (getterFunction != null) {
            desc.getter = this.getterFunction;
        }

        if (setterFunction != null) {
            desc.setter = this.setterFunction;
        } else if (es6) {
            desc.setter = Undefined.instance;
        }

        if (es6) {
            desc.enumerable = (attr & ScriptableObject.DONTENUM) == 0;
            desc.configurable = (attr & ScriptableObject.PERMANENT) == 0;
        }
        return desc;
    }

    @Override
    public boolean setValue(Object value, Scriptable scope, Scriptable start, boolean isThrow) {
        if (setter == null) {
            if (getter != null) {
                throwNoSetterException(start, value);
                return true;
            }
        } else {
            setter.accept(start, value);
            return true;
        }

        return super.setValue(value, start, start, isThrow);
    }

    @Override
    public Object getValue(Scriptable owner) {
        if (getter != null) {
            return getter.apply(owner);
        }
        return super.getValue(owner);
    }

    public void setGetter(Scriptable scope, ScriptableObject.LambdaGetterFunction getter) {
        this.getter = getter;
        if (getter != null) {
            this.getterFunction =
                    new LambdaFunction(
                            scope,
                            "get " + super.name,
                            0,
                            (cx1, scope1, thisObj, args) -> getter.apply(thisObj),
                            false);
        }
    }

    public void setSetter(Scriptable scope, ScriptableObject.LambdaSetterFunction setter) {
        this.setter = setter;
        if (setter != null) {
            this.setterFunction =
                    new LambdaFunction(
                            scope,
                            "set " + super.name,
                            1,
                            (cx1, scope1, thisObj, args) -> {
                                setter.accept(
                                        thisObj, args.length > 0 ? args[0] : Undefined.instance);
                                return Undefined.instance;
                            },
                            false);
        }
    }

    public void replaceWith(LambdaAccessorSlot slot) {
        this.getterFunction = slot.getterFunction;
        this.getter = slot.getter;
        this.setterFunction = slot.setterFunction;
        this.setter = slot.setter;
        setAttributes(slot.getAttributes());
    }
}
