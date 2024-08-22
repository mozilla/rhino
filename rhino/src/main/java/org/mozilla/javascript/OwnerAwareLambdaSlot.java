package org.mozilla.javascript;

import java.util.function.BiConsumer;
import java.util.function.Function;

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
public class OwnerAwareLambdaSlot extends Slot {
    private transient Function<Scriptable, Object> getter;
    private transient BiConsumer<Scriptable, Object> setter;

    OwnerAwareLambdaSlot(Object name, int index) {
        super(name, index, 0);
    }

    OwnerAwareLambdaSlot(Slot oldSlot) {
        super(oldSlot);
    }

    @Override
    boolean isValueSlot() {
        return false;
    }

    @Override
    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        if (getter != null) {
            desc.defineProperty(
                    "get",
                    new LambdaFunction(
                            scope,
                            "get " + super.name,
                            0,
                            (cx1, scope1, thisObj, args) -> getter.apply(thisObj)),
                    ScriptableObject.EMPTY);
        }

        if (setter != null) {
            desc.defineProperty(
                    "set",
                    new LambdaFunction(
                            scope,
                            "set " + super.name,
                            1,
                            (cx1, scope1, thisObj, args) -> {
                                setter.accept(thisObj, args[0]);
                                return Undefined.instance;
                            }),
                    ScriptableObject.EMPTY);
        }
        desc.setCommonDescriptorProperties(getAttributes(), false);
        return desc;
    }

    @Override
    public boolean setValue(Object value, Scriptable scope, Scriptable owner, boolean isStrict) {
        if (setter != null) {
            setter.accept(owner, value);
            return true;
        }

        if (isStrict) {
            // in strict mode
            throw ScriptRuntime.typeErrorById("msg.modify.readonly", name);
        } else {
            super.setValue(value, scope, owner, false);
        }

        return true;
    }

    @Override
    public Object getValue(Scriptable owner) {
        if (getter != null) {
            return getter.apply(owner);
        }
        Object v = super.getValue(owner);
        return v == null ? Undefined.instance : v;
    }

    public void setGetter(Function<Scriptable, Object> getter) {
        this.getter = getter;
    }

    public void setSetter(BiConsumer<Scriptable, Object> setter) {
        this.setter = setter;
    }
}
