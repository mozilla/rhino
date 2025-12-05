package org.mozilla.javascript.debug;

import org.mozilla.javascript.Context;

class NoOpDebugger implements Debugger {
    @Override
    public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
        return new NoOpDebugFrame();
    }

    private static class NoOpDebugFrame implements DebugFrame {}
}
