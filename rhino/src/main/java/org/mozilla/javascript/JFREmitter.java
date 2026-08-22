package org.mozilla.javascript;

import java.util.concurrent.atomic.AtomicInteger;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;

class JFREmitter extends InstrumentEmitter {

    private static final AtomicInteger INFLIGHT_COUNT;

    static {
        INFLIGHT_COUNT = new AtomicInteger();
        FlightRecorder.addListener(new RhinoRecordingListener());
        FlightRecorder.register(ParseEvent.class);
        FlightRecorder.register(InterpreterCompileEvent.class);
        FlightRecorder.register(ClassCompileEvent.class);
        FlightRecorder.register(ScriptExecEvent.class);
        FlightRecorder.register(ObjectsInitEvent.class);
        FlightRecorder.register(SafeObjectsInitEvent.class);
    }

    private static class RhinoRecordingListener implements FlightRecorderListener {
        @Override
        public void recorderInitialized(FlightRecorder recorder) {
            for (var recording : recorder.getRecordings()) {
                if (recording.getState() == RecordingState.RUNNING) {
                    INFLIGHT_COUNT.incrementAndGet();
                }
            }
        }

        @Override
        public void recordingStateChanged(Recording recording) {
            RecordingState state = recording.getState();
            if (state == RecordingState.RUNNING) {
                INFLIGHT_COUNT.incrementAndGet();
            } else if (state == RecordingState.STOPPED) {
                INFLIGHT_COUNT.decrementAndGet();
            }
        }
    }

    public JFREmitter() {}

    @Override
    public Object startEvent(Type type) {
        if (INFLIGHT_COUNT.get() == 0) {
            return null;
        }
        return buildEvent(type);
    }

    private static Object buildEvent(Type type) {
        return switch (type) {
            case PARSE -> new ParseEvent();
            case COMPILE_INTERPRETER -> new InterpreterCompileEvent();
            case COMPILE_CLASSFILE -> new ClassCompileEvent();
            case SCRIPT_EXEC -> new ScriptExecEvent();
            case SAFE_OBJECTS_INIT -> new SafeObjectsInitEvent();
            case OBJECTS_INIT -> new ObjectsInitEvent();
            case TEST_EVENT -> new TestEvent();
            default -> {
                throw new UnsupportedOperationException("Unsupported event type");
            }
        };
    }

    @Override
    public void endEvent(Object event, Object... data) {
        if (event != null) {
            ((RhinoEvent) event).fillAndSubmit(data);
        }
    }

    abstract static class RhinoEvent extends Event {
        abstract void fillAndSubmit(Object... data);
    }
}
