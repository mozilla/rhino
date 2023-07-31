/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.tools.shell.Global;

/**
 * Refer to https://github.com/mozilla/rhino/pull/1025
 *
 * @author Roland Praml, FOCONIS AG
 */
public class InitStandardObjectsTest {

    private Global global = new Global();
    private Context context;

    public InitStandardObjectsTest() {
        global.init(ContextFactory.getGlobal());
    }

    @Before
    public void setUp() throws Exception {
        context = ContextFactory.getGlobal().enterContext();
    }

    @After
    public void tearDown() throws Exception {
        context.close();
    }

    /**
     * This test calls initStandardObjects a second time. The expection is, that all modified
     * objects are reset to default. Due a caching issue <code>[] instanceof Object</code> was not
     * always true.
     */
    @Test
    public void initStandardObjects() {

        // Modify some prototypes
        exec("Array.prototype.toString = function() { return 'foo' }");
        assertEquals(true, exec("[] instanceof Array"));
        assertEquals(true, exec("[] instanceof Object"));
        assertEquals("foo", exec("[1,2,3].toString()"));

        // re-init standard objects (remove prototype modification)
        context.initStandardObjects(global);
        assertEquals("1,2,3", exec("[1,2,3].toString()"));
        assertEquals(true, exec("[] instanceof Array"));
        assertEquals(true, exec("[] instanceof Object"));
    }

    private Object exec(String source) {
        Script script = context.compileString(source, "", 1, null);
        return script.exec(context, global);
    }
}
