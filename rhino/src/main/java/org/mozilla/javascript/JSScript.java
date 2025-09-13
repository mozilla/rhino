package org.mozilla.javascript;

public class JSScript implements Script, ScriptOrFn<JSScript> {
    private final JSDescriptor<JSScript> descriptor;
    private final Scriptable homeObject;

    public JSScript(JSDescriptor<JSScript> descriptor, Scriptable homeObject) {
        this.descriptor = descriptor;
        this.homeObject = homeObject;
    }

    @Override
    public JSDescriptor<JSScript> getDescriptor() {
        return descriptor;
    }

    @Override
    public Scriptable getHomeObject() {
        return homeObject;
    }

    JSCode<JSScript> getCode() {
        return descriptor.getCode();
    }

    @Override
    public Object exec(Context cx, Scriptable scope, Scriptable thisObj) {
        Object ret;
        if (!ScriptRuntime.hasTopCall(cx)) {
            // It will go through "call" path. but they are equivalent
            ret = ScriptRuntime.doTopCall(this, cx, scope, thisObj, descriptor.isStrict());
        } else {
            ret =
                    descriptor
                            .getCode()
                            .execute(cx, this, null, scope, thisObj, ScriptRuntime.emptyArgs);
        }
        cx.processMicrotasks();
        return ret;
    }
}
