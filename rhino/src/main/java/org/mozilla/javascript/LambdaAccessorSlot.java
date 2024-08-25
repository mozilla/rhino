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
public class LambdaAccessorSlot extends Slot {
    private transient Function<Scriptable, Object> getter;
    private transient BiConsumer<Scriptable, Object> setter;
    private LambdaFunction getterFunction;
    private LambdaFunction setterFunction;

    LambdaAccessorSlot(Object name, int index) {
        super(name, index, 0);
    }

    LambdaAccessorSlot(Slot oldSlot) {
        super(oldSlot);
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
    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);

        int attr = getAttributes();
        boolean es6 = cx.getLanguageVersion() >= Context.VERSION_ES6;
        if (es6) {
            if (getterFunction == null && setterFunction == null) {
                desc.defineProperty(
                        "writable",
                        (attr & ScriptableObject.READONLY) == 0,
                        ScriptableObject.EMPTY);
            }
        } else {
            desc.setCommonDescriptorProperties(
                    attr, getterFunction == null && setterFunction == null);
        }

        if (getterFunction != null) {
            desc.defineProperty("get", this.getterFunction, ScriptableObject.EMPTY);
        }

        if (setterFunction != null) {
            desc.defineProperty("set", this.setterFunction, ScriptableObject.EMPTY);
        } else if (es6) {
            desc.defineProperty("set", Undefined.instance, ScriptableObject.EMPTY);
        }

        if (es6) {
            desc.defineProperty(
                    "enumerable", (attr & ScriptableObject.DONTENUM) == 0, ScriptableObject.EMPTY);
            desc.defineProperty(
                    "configurable",
                    (attr & ScriptableObject.PERMANENT) == 0,
                    ScriptableObject.EMPTY);
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

    public void setGetter(Scriptable scope, Function<Scriptable, Object> getter) {
        this.getter = getter;
        if (getter != null) {
            this.getterFunction =
                    new LambdaFunction(
                            scope,
                            "get " + super.name,
                            0,
                            (cx1, scope1, thisObj, args) -> getter.apply(thisObj));
        }
    }

    public void setSetter(Scriptable scope, BiConsumer<Scriptable, Object> setter) {
        this.setter = setter;
        if (setter != null) {
            this.setterFunction =
                    new LambdaFunction(
                            scope,
                            "set " + super.name,
                            1,
                            (cx1, scope1, thisObj, args) -> {
                                setter.accept(thisObj, args[0]);
                                return Undefined.instance;
                            });
        }
    }
}
