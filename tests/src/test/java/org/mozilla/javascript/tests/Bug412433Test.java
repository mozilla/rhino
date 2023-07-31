/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/** */
package org.mozilla.javascript.tests;

import org.junit.Test;
import org.mozilla.javascript.ScriptableObject;

/**
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=412433
 *
 * @author Norris Boyd
 */
public class Bug412433Test {

    @Test
    public void malformedJavascript2() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    ScriptableObject scope = cx.initStandardObjects();
                    cx.evaluateString(scope, "\"\".split(/[/?,/&]/)", "", 0, null);

                    return null;
                });
    }
}
