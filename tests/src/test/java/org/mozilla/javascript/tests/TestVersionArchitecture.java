package test;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class TestVersionArchitecture {

    @Test
    public void testMapInES5() {
        Utils.assertWithAllModes_1_8("undefined", "typeof Map");
    }

    @Test
    public void testMapInES6() {
        Utils.assertWithAllModes_ES6("function", "typeof Map");
    }

    @Test
    public void testWeakRefInES5() {
        Utils.assertWithAllModes_1_8("undefined", "typeof WeakRef");
    }

    @Test
    public void testWeakRefInES6() {
        Utils.assertWithAllModes_ES6("function", "typeof WeakRef");
    }
}
