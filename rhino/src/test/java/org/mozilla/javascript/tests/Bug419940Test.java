/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.Test;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=419940
 *
 * @author Norris Boyd
 */
public class Bug419940Test {
    public abstract static class BaseFoo {
        public abstract int doSomething();
    }

    public static class Foo extends BaseFoo {
        @Override
        public int doSomething() {
            return 12;
        }
    }

    @Test
    public void adapter() {
        String script = "(new JavaAdapter(" + Foo.class.getName() + ", {})).doSomething();";

        Utils.assertWithAllModes(12, script);
    }
}
