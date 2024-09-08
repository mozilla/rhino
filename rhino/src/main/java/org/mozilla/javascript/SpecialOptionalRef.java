package org.mozilla.javascript;

class SpecialOptionalRef extends Ref {
    Ref specialRef;

    private SpecialOptionalRef(Ref specialRef) {
        this.specialRef = specialRef;
    }

    public static Ref create(Context cx, Scriptable scope, Object object, String name) {
        Scriptable target = ScriptRuntime.toObjectOrNull(cx, object, scope);
        if (target != null && target != Undefined.instance) {
            return new SpecialOptionalRef(SpecialRef.createSpecial(cx, scope, object, name));
        }
        return new SpecialOptionalRef(null);
    }

    @Override
    public Object get(Context cx) {
        if (specialRef == null) return Undefined.instance;
        return specialRef.get(cx);
    }

    @Override
    public Object set(Context cx, Object value) {
        throw new IllegalStateException();
    }
}
