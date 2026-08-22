package org.mozilla.javascript;

import java.lang.reflect.Constructor;

public abstract class InstrumentEmitter {

    public enum Type {
        PARSE,
        COMPILE_INTERPRETER,
        COMPILE_CLASSFILE,
        SCRIPT_EXEC,
        SAFE_OBJECTS_INIT,
        OBJECTS_INIT,
        TEST_EVENT
    }

    public static final InstrumentEmitter emitter;

    static {
        InstrumentEmitter emitterTmp;
        try {
            Class<?> jfrClass = Class.forName("jdk.jfr.Event");
            if (jfrClass != null) {
                @SuppressWarnings("unchecked")
                var c =
                        (Constructor<? extends InstrumentEmitter>)
                                Class.forName("org.mozilla.javascript.JFREmitter").getConstructor();
                emitterTmp = c.newInstance();
            } else {
                emitterTmp = new NullEmitter();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            emitterTmp = new NullEmitter();
        }
        emitter = emitterTmp;
    }

    public abstract Object startEvent(Type type);

    public abstract void endEvent(Object event, Object... data);

    private static class NullEmitter extends InstrumentEmitter {

        private NullEmitter() {}

        @Override
        public Object startEvent(Type type) {
            return null;
        }

        public void endEvent(Object event, Object... data) {
            // Do nothing here.
        }
    }
}
