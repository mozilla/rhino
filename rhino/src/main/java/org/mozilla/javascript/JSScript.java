package org.mozilla.javascript;

/**
 * Represents a script object built upon a {@link JSDescriptor}. This class does not support the
 * {@link Callable} interface scripts do not support arguments or some other parts of a call
 * operation.
 */
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
            cx.processMicrotasks();
        } else {
            ret =
                    descriptor
                            .getCode()
                            .execute(cx, this, null, scope, thisObj, ScriptRuntime.emptyArgs);
        }
        return ret;
    }
}
