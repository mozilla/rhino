load("testsrc/assert.js");

assertEquals(Math.sign(NaN), NaN);
assertEquals(Math.sign(0), 0);
assertEquals(Math.sign(-0), -0);
assertEquals(Math.sign(Infinity), 1);
assertEquals(Math.sign(-Infinity), -1);
assertEquals(Math.sign(5), 1);
assertEquals(Math.sign(-5), -1);
assertEquals(Math.sign(0.5), 1);
assertEquals(Math.sign(-0.5), -1);
assertEquals(Math.sign(Number.MIN_VALUE), 1);
assertEquals(Math.sign(-Number.MIN_VALUE), -1);
assertEquals(Math.sign(Number.MAX_VALUE), 1);
assertEquals(Math.sign(-Number.MAX_VALUE), -1);

"success";
