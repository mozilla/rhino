/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 *
 */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.*;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=409702
 * @author Norris Boyd
 */
public class Bug409702Test extends TestCase {

    public static abstract class Foo {
        public Foo() {
        }

        public abstract void a();

        public abstract int b();

        public static abstract class Subclass extends Foo {

            @Override
            public final void a() {
            }
        }
    }

  public void testAdapter() {
      final int value = 12;
      String source =
          "var instance = " +
          "  new JavaAdapter(" + Foo.Subclass.class.getName() + "," +
          "{ b: function () { return " + value + "; } });" +
          "instance.b();";

      Context cx = ContextFactory.getGlobal().enterContext();
      try {
          Scriptable scope = cx.initStandardObjects();
          Object result = cx.evaluateString(scope, source, "source", 1, null);
          assertEquals(new Integer(value), result);
      } finally {
          Context.exit();
      }
  }
}
