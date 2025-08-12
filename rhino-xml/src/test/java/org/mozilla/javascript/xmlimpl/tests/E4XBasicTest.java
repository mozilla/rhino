/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.xmlimpl.tests;

import static org.mozilla.javascript.Context.FEATURE_E4X;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.testutils.Utils;

/** A basic test for E4X. */
public class E4XBasicTest {
    @Test
    public void smokeTest() {
        String source =
                "var v = <doc name=\"theName\"><body><text>theText</text></body></doc>;\n"
                        + "v.@name + ':' + v.*.text";
        Utils.assertWithAllModes("theName:theText", source);
    }

    @Test
    public void parseErrorIfFeatureIsDisabledOnAttribute() {
        String source = "v.@name";

        var contextFactoryNoE4X = Utils.contextFactoryWithFeatureDisabled(FEATURE_E4X);
        Utils.assertException(
                contextFactoryNoE4X,
                Context.VERSION_ECMASCRIPT,
                EvaluatorException.class,
                "missing name after . operator (test#1)",
                source);
    }

    @Test
    public void parseErrorIfFeatureIsDisabledOnStar() {
        String source = "v.*.text";

        var contextFactoryNoE4X = Utils.contextFactoryWithFeatureDisabled(FEATURE_E4X);
        Utils.assertException(
                contextFactoryNoE4X,
                Context.VERSION_ECMASCRIPT,
                EvaluatorException.class,
                "missing name after . operator (test#1)",
                source);
    }
}
