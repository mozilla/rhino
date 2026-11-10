/* -*- Mode: javascript; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '13.5.3.js';

START("13.5.3 - XMLList Constructor Properties");

var settingsProps = [
    "defaultSettings",
    "settings",
    "setSettings",
    "prettyPrinting",
    "ignoreWhitespace",
    "ignoreProcessingInstructions",
    "ignoreComments"
];

for (var i = 0; i < settingsProps.length; i++) {
    var p = settingsProps[i];
    TEST(2 * i + 1, false, p in XMLList);
    TEST(2 * i + 2, "undefined", typeof XMLList[p]);
}

END();
