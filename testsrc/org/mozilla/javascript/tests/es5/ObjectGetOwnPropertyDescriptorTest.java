/*
 * Tests for the Object.getOwnPropertyDescriptor(obj, prop) method
 */
package org.mozilla.javascript.tests.es5;
import org.mozilla.javascript.*;
import static org.mozilla.javascript.tests.Evaluator.eval;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ObjectGetOwnPropertyDescriptorTest {

  @Test
  public void testContentsOfPropertyDescriptorShouldReflectAttributesOfProperty() {
    NativeObject descriptor;
    NativeObject object = new NativeObject();
    object.defineProperty("a", "1", ScriptableObject.EMPTY, false);
    object.defineProperty("b", "2", ScriptableObject.DONTENUM | ScriptableObject.READONLY | ScriptableObject.PERMANENT, false);

    descriptor = (NativeObject) eval("Object.getOwnPropertyDescriptor(obj, 'a')", "obj", object);
    assertEquals("1",  descriptor.get("value"));
    assertEquals(true, descriptor.get("enumerable"));
    assertEquals(true, descriptor.get("writable"));
    assertEquals(true, descriptor.get("configurable"));

    descriptor = (NativeObject) eval("Object.getOwnPropertyDescriptor(obj, 'b')", "obj", object);
    assertEquals("2",  descriptor.get("value"));
    assertEquals(false, descriptor.get("enumerable"));
    assertEquals(false, descriptor.get("writable"));
    assertEquals(false, descriptor.get("configurable"));
  }

}
