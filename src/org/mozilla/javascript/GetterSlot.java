package org.mozilla.javascript;

/**
 * This is a specialization of Slot to store various types of values that are retrieved dynamically
 * using Java and JavaScript functions. Unlike LambdaSlot, the fact that these values are accessed
 * and mutated by functions is visible via the slot's property descriptor.
 */
public class GetterSlot extends Slot {
    private static final long serialVersionUID = 1677840254177335827L;

    GetterSlot(Slot oldSlot) {
        super(oldSlot);
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
    ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
        // It sounds logical that this would be the same as the logic for a normal Slot,
        // but the spec is super pedantic about things like the order of properties here,
        // so we need special support here.
        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        desc.setCommonDescriptorProperties(getAttributes(), getter == null && setter == null);
        String fName = name == null ? "f" : name.toString();
        if (getter != null) {
            Function f = getter.asGetterFunction(fName, scope);
            desc.defineProperty("get", f == null ? Undefined.instance : f, ScriptableObject.EMPTY);
        }
        if (setter != null) {
            Function f = setter.asSetterFunction(fName, scope);
            desc.defineProperty("set", f == null ? Undefined.instance : f, ScriptableObject.EMPTY);
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
            return setter.setValue(value, owner, start);
        }
        return super.setValue(value, owner, start);
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

    abstract static class Getter {
        abstract Object getValue(Scriptable start);

        abstract Function asGetterFunction(final String name, final Scriptable scope);
    }

    /** This is a Getter that delegates to a Java function via a MemberBox. */
    static final class MemberBoxGetter extends Getter {
        final MemberBox member;

        MemberBoxGetter(MemberBox member) {
            this.member = member;
        }

        Object getValue(Scriptable start) {
            if (member.delegateTo == null) {
                return member.invoke(start, ScriptRuntime.emptyArgs);
            }
            return member.invoke(member.delegateTo, new Object[] {start});
        }

        Function asGetterFunction(String name, Scriptable scope) {
            return member.asGetterFunction(name, scope);
        }
    }

    /** This is a getter that delegates to a JavaScript function. */
    static final class FunctionGetter extends Getter {
        // The value of the function might actually be Undefined, so we need an Object here.
        final Object target;

        FunctionGetter(Object target) {
            this.target = target;
        }

        Object getValue(Scriptable start) {
            if (target instanceof Function) {
                Function t = (Function) target;
                Context cx = Context.getContext();
                return t.call(cx, t.getParentScope(), start, ScriptRuntime.emptyArgs);
            }
            return Undefined.instance;
        }

        Function asGetterFunction(String name, Scriptable scope) {
            return target instanceof Function ? (Function) target : null;
        }
    }

    abstract static class Setter {
        abstract boolean setValue(Object value, Scriptable owner, Scriptable start);

        abstract Function asSetterFunction(final String name, final Scriptable scope);
    }

    /** Invoke the setter on this slot via reflection using MemberBox. */
    static final class MemberBoxSetter extends Setter {
        final MemberBox member;

        MemberBoxSetter(MemberBox member) {
            this.member = member;
        }

        boolean setValue(Object value, Scriptable owner, Scriptable start) {
            Context cx = Context.getContext();
            Class<?>[] pTypes = member.argTypes;
            // XXX: cache tag since it is already calculated in
            // defineProperty ?
            Class<?> valueType = pTypes[pTypes.length - 1];
            int tag = FunctionObject.getTypeTag(valueType);
            Object actualArg = FunctionObject.convertArg(cx, start, value, tag);

            if (member.delegateTo == null) {
                member.invoke(start, new Object[] {actualArg});
            } else {
                member.invoke(member.delegateTo, new Object[] {start, actualArg});
            }
            return true;
        }

        Function asSetterFunction(String name, Scriptable scope) {
            return member.asSetterFunction(name, scope);
        }
    }

    /**
     * Invoke the setter as a JavaScript function, taking care that it might actually be Undefined.
     */
    static final class FunctionSetter extends Setter {
        final Object target;

        FunctionSetter(Object target) {
            this.target = target;
        }

        boolean setValue(Object value, Scriptable owner, Scriptable start) {
            if (target instanceof Function) {
                Function t = (Function) target;
                Context cx = Context.getContext();
                t.call(cx, t.getParentScope(), start, new Object[] {value});
            }
            return true;
        }

        Function asSetterFunction(String name, Scriptable scope) {
            return target instanceof Function ? (Function) target : null;
        }
    }
}
