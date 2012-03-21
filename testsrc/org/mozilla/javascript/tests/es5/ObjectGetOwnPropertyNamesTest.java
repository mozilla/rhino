/*
 * Tests for the Object.getOwnPropertyNames(obj) method
 */
package org.mozilla.javascript.tests.es5;
import org.mozilla.javascript.*;
import static org.mozilla.javascript.tests.Evaluator.eval;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ObjectGetOwnPropertyNamesTest {

  @Test
  public void testShouldReturnAllPropertiesOfArg() {
    NativeObject object = new NativeObject();
    object.defineProperty("a", "1", ScriptableObject.EMPTY, false);
    object.defineProperty("b", "2", ScriptableObject.DONTENUM, false);

    Object result = eval("Object.getOwnPropertyNames(obj)", "obj", object);

    NativeArray names = (NativeArray) result;

    assertEquals(2, names.getLength());
    assertEquals("a", names.get(0, names));
    assertEquals("b", names.get(1, names));
  }

}
