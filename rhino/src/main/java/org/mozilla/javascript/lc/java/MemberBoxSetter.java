package org.mozilla.javascript.lc.java;

import org.mozilla.javascript.AccessorSlot;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/** Invoke the setter on this slot via reflection using MemberBox. */
final class MemberBoxSetter implements AccessorSlot.Setter {
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
        var valueType = pTypes.get(pTypes.size() - 1);
        boolean isNullable = member.getArgNullability().isNullable(pTypes.size() - 1);
        int tag = valueType.getTypeTag();
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
        return member.asSetterFunction(name, scope);
    }

    @Override
    public boolean isSameSetterFunction(Object function) {
        return member.isSameSetterFunction(function);
    }
}
