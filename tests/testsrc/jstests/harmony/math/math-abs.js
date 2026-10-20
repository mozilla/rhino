load("testsrc/assert.js");

assertEquals(Math.abs(NaN), NaN);
assertEquals(Math.abs(0), 0);
assertEquals(Math.abs(-0), 0);
assertEquals(Math.abs(Infinity), Infinity);
assertEquals(Math.abs(-Infinity), Infinity);
assertEquals(Math.abs(1), 1);
assertEquals(Math.abs(-1), 1);
assertEquals(Math.abs(1.5), 1.5);
assertEquals(Math.abs(-1.5), 1.5);
assertEquals(Math.abs(Number.MAX_VALUE), Number.MAX_VALUE);
assertEquals(Math.abs(-Number.MAX_VALUE), Number.MAX_VALUE);
assertEquals(Math.abs(Number.MIN_VALUE), Number.MIN_VALUE);
assertEquals(Math.abs(-Number.MIN_VALUE), Number.MIN_VALUE);
assertEquals(Math.abs(-4503599627370496), 4503599627370496);

"success";
