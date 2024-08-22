package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertFalse;
import static org.mozilla.javascript.tests.Evaluator.eval;

import org.junit.Test;
import org.mozilla.javascript.NativeObject;

/**
 * When calling <b>propertyIsEnumerable</b> on a <b>String</b>, missing properties should return
 * <b>false</b> instead of throwing a <b>Property {0} not found.</b> exception.
 *
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/415">https://github.com/mozilla/rhino/issues/415</a>
 */
public class StringMissingPropertyIsNotEnumerableTest {

    @Test
    public void stringMissingPropertyIsNotEnumerable() {
        NativeObject object = new NativeObject();

        Object result = eval("'s'.propertyIsEnumerable(0)", "obj", object);

        assertFalse((Boolean) result);
    }
}
