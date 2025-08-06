package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class ApplyTest {

    @Test
    public void invokeBoundCallManyArgs() {
        /* This test is a little fiddly. The call to bind causes the
        max stack size to be high enough that it could mask the
        bug, so we have to make the call to the bound in function
        in another function which has a smaller stsck. */
        String code =
                "Function.prototype.apply.apply(function(x) {return x;}, ['b', ['Hello!', 'Goodbye!']]);";

        Utils.assertWithAllModes_ES6("Hello!", code);
    }
}
