/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import org.junit.Test;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=409702
 *
 * @author Norris Boyd
 */
public class Bug409702Test {

    public abstract static class Foo {
        public Foo() {}

        public abstract void a();

        public abstract int b();

        public abstract static class Subclass extends Foo {

            @Override
            public final void a() {}
        }
    }

    @Test
    public void adapter() {
        String script =
                "var instance = new JavaAdapter("
                        + Foo.Subclass.class.getName()
                        + ","
                        + "{ b: function () { return 12; } });"
                        + "instance.b();";

        Utils.assertWithAllModes(12, script);
    }
}
