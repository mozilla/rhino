/*---
https://tc39.es/ecma262/#sec-math.max
https://tc39.es/ecma262/#sec-math.min

esid: sec-math.max, sec-math.min
description: Call ToNumber on each element of params
info: |
    2. For each element arg of args, do
        Let n be ? ToNumber(arg).
        Append n to coerced.
---*/

load("testsrc/assert.js");

/*
Math.max()
*/
var valueOf_callsFromMax = 0;

var nForMax = {
  valueOf: function() {
    valueOf_callsFromMax++;
  }
};
Math.max(NaN, nForMax);
assertEquals(1, valueOf_callsFromMax);

/*
Math.min()
*/
var valueOf_callsFromMin = 0;

var nForMin = {
  valueOf: function() {
    valueOf_callsFromMin++;
  }
};
Math.min(NaN, nForMin);
assertEquals(1, valueOf_callsFromMin);

"success"
