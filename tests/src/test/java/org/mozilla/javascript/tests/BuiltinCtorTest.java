package org.mozilla.javascript.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;

public class BuiltinCtorTest {

    @Test
    public void checkCtorArity() {
        try (Context cx = Context.enter()) {
            TopLevel tl = new TopLevel();
            cx.initSafeStandardObjects(tl);
            for (var bi : TopLevel.Builtins.values()) {
                var ctor = tl.getBuiltinCtor(bi);
                if (ctor.isConstructor()) {

                    Object res = null;
                    try {
                        res = ctor.construct(cx, tl, new Object[0]);
                    } catch (Exception e) {
                        // Some constructors do throw a type error.
                    }
                    if (res == null) {
                        continue;
                    }
                    assertTrue(
                            String.format("Expected %s() to return an object", bi.name()),
                            res instanceof Scriptable);
                    assertNotNull(
                            String.format("Expected %s() to set parent scope", bi.name()),
                            ((Scriptable) res).getParentScope());
                }
            }
        }
    }
}
