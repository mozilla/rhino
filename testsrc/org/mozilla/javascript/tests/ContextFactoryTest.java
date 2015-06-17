/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 *
 */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.RhinoException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(BlockJUnit4ClassRunner.class)
public class ContextFactoryTest {

    @Test
    public void whenVersionLessEq17ThenOldUndefNullThis() throws Exception {
        Context ctx = Context.enter();
        ctx.setLanguageVersion(Context.VERSION_1_7);
        assertTrue(ctx.hasFeature(Context.FEATURE_OLD_UNDEF_NULL_THIS));

        ctx.setLanguageVersion(Context.VERSION_1_6);
        assertTrue(ctx.hasFeature(Context.FEATURE_OLD_UNDEF_NULL_THIS));
    }

    @Test
    public void whenVersionGt17ThenNewUndefNullThis() throws Exception {
        Context ctx = Context.enter();
        ctx.setLanguageVersion(Context.VERSION_1_8);
        assertFalse(ctx.hasFeature(Context.FEATURE_OLD_UNDEF_NULL_THIS));

        ctx.setLanguageVersion(Context.VERSION_ES6);
        assertFalse(ctx.hasFeature(Context.FEATURE_OLD_UNDEF_NULL_THIS));
    }

}
