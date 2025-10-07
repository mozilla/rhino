package org.mozilla.javascript.tests.type_info.test_object;

import java.io.Serializable;

/**
 * @author ZZZank
 */
public class GenericBean<M extends Number> implements Serializable {
    private static final long serialVersionUID = 1L;

    private M value;

    public M publicValue;

    private M valueMultipleSetters;

    public M getValue() {
        return value;
    }

    public void setValue(M value) {
        this.value = value;
    }

    public M getValueMultipleSetters() {
        return valueMultipleSetters;
    }

    public void setValueMultipleSetters(M valueMultipleSetters) {
        this.valueMultipleSetters = valueMultipleSetters;
    }

    public void setValueMultipleSetters(String s) {
        throw new UnsupportedOperationException("Should not be called");
    }
}
