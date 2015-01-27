/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-302097.js';

var summary = 'E4X - Function.prototype.toString should not quote {} ' +
    'attribute values';
var BUGNUMBER = 302097;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

function f(k) {
  return <xml k={k}/>;
}

actual = f.toString().replace(/</g, '&lt;');
expect = 'function f(k) {\n    return &lt;xml k={k}/>;\n}';

TEST(1, expect, actual);

END();
