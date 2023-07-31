// https://github.com/mozilla/rhino/issues/842
load("testsrc/assert.js");

assertEquals(new Number(1), Object.assign(1, 2, 3, 4));
assertEquals(new Number(1), Object.assign.apply(null, [1, 2, 3, 4]));

"success";
