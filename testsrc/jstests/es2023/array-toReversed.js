// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

const array1=[1,2,3];
assertArrayEquals([3,2,1], array1.toReversed());
assertFalse(arrayEquals(array1,array1.toReversed()));

const array2 = ["321","abc", "def","fgh"];
assertArrayEquals(["fgh", "def","abc","321"], array2.toReversed());
assertFalse(arrayEquals(array2,array2.toReversed()));

const array3 = ["321","", "","o"];
assertArrayEquals(["o", "","","321"], array3.toReversed());
assertFalse(arrayEquals(array3,array3.toReversed()));

const array4 = ["1",,,"d"];
assertArrayEquals(["d",,,"1"], array4.toReversed());
assertFalse(arrayEquals(array4,array4.toReversed()));

function arrayEquals(a, b) {
    return Array.isArray(a) &&
        Array.isArray(b) &&
        a.length === b.length &&
        a.every((val, index) => val === b[index]);
}


"success"