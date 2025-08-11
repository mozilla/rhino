package org.mozilla.javascript.ast;

/** Property of an object literal. */
public abstract class AbstractObjectProperty extends AstNode {
    protected AbstractObjectProperty() {
        super();
    }

    protected AbstractObjectProperty(int pos) {
        super(pos);
    }

    protected AbstractObjectProperty(int pos, int len) {
        super(pos, len);
    }
}
