package org.mozilla.javascript.lc.java;

import org.mozilla.javascript.AccessorSlot;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/** This is a Getter that delegates to a Java function via a MemberBox. */
final class MemberBoxGetter implements AccessorSlot.Getter {
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
        return member.asGetterFunction(name, scope);
    }

    @Override
    public boolean isSameGetterFunction(Object function) {
        return member.isSameGetterFunction(function);
    }
}
