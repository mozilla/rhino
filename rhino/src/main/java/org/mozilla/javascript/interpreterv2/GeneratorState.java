package org.mozilla.javascript.interpreterv2;

public class GeneratorState {
    public GeneratorState(int operation, Object value) {
        this.operation = operation;
        this.value = value;
    }

    public int operation;
    public Object value;
    public RuntimeException returnedException;
}
