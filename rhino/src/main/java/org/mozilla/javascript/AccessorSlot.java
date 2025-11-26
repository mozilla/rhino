package org.mozilla.javascript;

import org.mozilla.javascript.ScriptableObject.DescriptorInfo;

/**
 * This is a specialization of Slot to store various types of values that are retrieved dynamically
 * using Java and JavaScript functions. Unlike LambdaSlot, the fact that these values are accessed
 * and mutated by functions is visible via the slot's property descriptor.
 */
public class AccessorSlot extends Slot {
    private static final long serialVersionUID = 1677840254177335827L;

    AccessorSlot(Object name, int index) {
        super(name, index, 0);
    }

    AccessorSlot(Slot oldSlot) {
        super(oldSlot);
    }

    @Override
    AccessorSlot copySlot() {
        var newSlot = new AccessorSlot(this);
        newSlot.value = value;
        newSlot.getter = getter;
        newSlot.setter = setter;
        newSlot.next = null;
        newSlot.orderedNext = null;
        return newSlot;
    }

    // The Getter and Setter may each be of a different type (JavaScript function, Java
    // function, or neither). So, use an abstraction to distinguish them.
    transient Getter getter;
    transient Setter setter;

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
        // It sounds logical that this would be the same as the logic for a normal Slot,
        // but the spec is super pedantic about things like the order of properties here,
        // so we need special support here.

        int attr = getAttributes();
        DescriptorInfo desc;
        boolean es6 = cx.getLanguageVersion() >= Context.VERSION_ES6;
        if (es6) {
            desc = new DescriptorInfo(ScriptableObject.NOT_FOUND, attr, false);
            if (getter == null && setter == null) {
                desc.writable = (attr & ScriptableObject.READONLY) == 0;
            }
        } else {
            desc =
                    new DescriptorInfo(
                            ScriptableObject.NOT_FOUND, attr, getter == null && setter == null);
        }

        String fName = name == null ? "f" : name.toString();
        if (getter != null) {
            Function f = getter.asGetterFunction(fName, scope);
            desc.getter = f == null ? Undefined.instance : f;
        }
        if (setter != null) {
            Function f = setter.asSetterFunction(fName, scope);
            desc.setter = f == null ? Undefined.instance : f;
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
    public boolean setValue(Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        if (setter == null) {
            if (getter != null) {
                throwNoSetterException(start, value);
                return true;
            }
        } else {
            return setter.setValue(value, owner, start);
        }
        return super.setValue(value, owner, start, isThrow);
    }

    @Override
    public Object getValue(Scriptable start) {
        if (getter != null) {
            return getter.getValue(start);
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

    @Override
    boolean isSameGetterFunction(Object function) {
        if (function == Scriptable.NOT_FOUND) {
            return true;
        }
        if (getter == null) {
            return ScriptRuntime.shallowEq(Undefined.instance, function);
        }
        return getter.isSameGetterFunction(function);
    }

    @Override
    boolean isSameSetterFunction(Object function) {
        if (function == Scriptable.NOT_FOUND) {
            return true;
        }
        if (setter == null) {
            return ScriptRuntime.shallowEq(Undefined.instance, function);
        }
        return setter.isSameSetterFunction(function);
    }

    interface Getter {
        Object getValue(Scriptable start);

        Function asGetterFunction(final String name, final Scriptable scope);

        boolean isSameGetterFunction(Object getter);
    }

    /** This is a Getter that delegates to a Java function via a MemberBox. */
    static final class MemberBoxGetter implements Getter {
        final MemberBox member;

        MemberBoxGetter(MemberBox member) {
            this.member = member;
        }

        @Override
        public Object getValue(Scriptable start) {
            if (member.delegateTo == null) {
                return member.invoke(start, ScriptRuntime.emptyArgs);
            }
            return member.invoke(member.delegateTo, new Object[] {start});
        }

        @Override
        public Function asGetterFunction(String name, Scriptable scope) {
            return member.asGetterFunction(name);
        }

        @Override
        public boolean isSameGetterFunction(Object function) {
            return member.isSameGetterFunction(function);
        }
    }

    /** This is a getter that delegates to a JavaScript function. */
    static final class FunctionGetter implements Getter {
        // The value of the function might actually be Undefined, so we need an Object here.
        final Object target;

        FunctionGetter(Object target) {
            this.target = target;
        }

        @Override
        public Object getValue(Scriptable start) {
            if (target instanceof Function) {
                Function t = (Function) target;
                Context cx = Context.getContext();
                return t.call(cx, t.getDeclarationScope(), start, ScriptRuntime.emptyArgs);
            }
            return Undefined.instance;
        }

        @Override
        public Function asGetterFunction(String name, Scriptable scope) {
            return target instanceof Function ? (Function) target : null;
        }

        @Override
        public boolean isSameGetterFunction(Object function) {
            return ScriptRuntime.shallowEq(
                    target instanceof Function ? (Function) target : Undefined.instance, function);
        }
    }

    interface Setter {
        boolean setValue(Object value, Scriptable owner, Scriptable start);

        Function asSetterFunction(final String name, final Scriptable scope);

        boolean isSameSetterFunction(Object getter);
    }

    /** Invoke the setter on this slot via reflection using MemberBox. */
    static final class MemberBoxSetter implements Setter {
        final MemberBox member;

        MemberBoxSetter(MemberBox member) {
            this.member = member;
        }

        @Override
        public boolean setValue(Object value, Scriptable owner, Scriptable start) {
            Context cx = Context.getContext();
            var pTypes = member.getArgTypes();
            // XXX: cache tag since it is already calculated in
            // defineProperty ?
            var valueType = pTypes[pTypes.length - 1];
            boolean isNullable = member.getArgNullability().isNullable(pTypes.length - 1);
            int tag = FunctionObject.getTypeTag(valueType);
            Object actualArg = FunctionObject.convertArg(cx, start, value, tag, isNullable);

            if (member.delegateTo == null) {
                member.invoke(start, new Object[] {actualArg});
            } else {
                member.invoke(member.delegateTo, new Object[] {start, actualArg});
            }
            return true;
        }

        @Override
        public Function asSetterFunction(String name, Scriptable scope) {
            return member.asSetterFunction(name);
        }

        @Override
        public boolean isSameSetterFunction(Object function) {
            return member.isSameSetterFunction(function);
        }
    }

    /**
     * Invoke the setter as a JavaScript function, taking care that it might actually be Undefined.
     */
    static final class FunctionSetter implements Setter {
        final Object target;

        FunctionSetter(Object target) {
            this.target = target;
        }

        @Override
        public boolean setValue(Object value, Scriptable owner, Scriptable start) {
            if (target instanceof Function) {
                Function t = (Function) target;
                Context cx = Context.getContext();
                t.call(cx, t.getDeclarationScope(), start, new Object[] {value});
            }
            return true;
        }

        @Override
        public Function asSetterFunction(String name, Scriptable scope) {
            return target instanceof Function ? (Function) target : null;
        }

        @Override
        public boolean isSameSetterFunction(Object function) {
            return ScriptRuntime.shallowEq(
                    target instanceof Function ? (Function) target : Undefined.instance, function);
        }
    }
}
