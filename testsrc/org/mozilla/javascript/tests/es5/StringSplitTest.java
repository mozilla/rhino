package org.mozilla.javascript.tests.es5;

import static org.junit.Assert.assertEquals;
import static org.mozilla.javascript.tests.Evaluator.eval;

import org.junit.Test;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

/**
 * When calling <b>propertyIsEnumerable</b> on a <b>String</b>, missing properties should return
 * <b>false</b> instead of throwing a <b>Property {0} not found.</b> exception.
 *
 * @see <a
 *     href="https://github.com/mozilla/rhino/issues/415">https://github.com/mozilla/rhino/issues/415</a>
 */
public class StringSplitTest {

    @Test
    public void splitLimitZero() {
        NativeObject object = new NativeObject();

        NativeArray result =
                (NativeArray) eval("'123 456 789'.split(undefined, 0);", "obj", object);
        assertEquals(0, result.size());
    }

    @Test
    public void splitLimitNegative() {
        NativeObject object = new NativeObject();

        NativeArray result =
                (NativeArray) eval("'123 456 789'.split(undefined, -1);", "obj", object);
        assertEquals(1, result.size());
        assertEquals("123 456 789", result.get(0));

        result = (NativeArray) eval("'123 456 789'.split(undefined, -4);", "obj", object);
        assertEquals(1, result.size());
        assertEquals("123 456 789", result.get(0));
    }
}
