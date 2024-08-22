/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

function f1(x, y) {
    return f2(y, x)
}

function f2(x, y) {
    return f3(x, x)
}

function f3(x, y) {
    capture()
}

f1("a", 5)
