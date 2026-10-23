load("testsrc/assert.js");

assertEquals(Math.hypot(), 0);
assertEquals(Math.hypot(3, 4), 5);
assertEquals(Math.hypot(3, 4, 5), 7.0710678118654755);
assertEquals(Math.hypot(-3), 3);
assertEquals(Math.hypot(-0), 0);
assertEquals(Math.hypot(0, -0), 0);
assertEquals(Math.hypot(-0, -0), 0);
assertEquals(Math.hypot(NaN), NaN);
assertEquals(Math.hypot(NaN, Infinity), Infinity);
assertEquals(Math.hypot(Infinity, NaN), Infinity);
assertEquals(Math.hypot(-Infinity, NaN), Infinity);
assertEquals(Math.hypot(1, NaN), NaN);
assertEquals(Math.hypot(3e+300, 4e+300), 5e+300);
assertEquals(Math.hypot(1e+200, 1e+200), 1.414213562373095e+200);
assertEquals(Math.hypot(1e-200, 1e-200), 1.414213562373095e-200);
assertEquals(Math.hypot(3e-160, 4e-160), 5e-160);
assertEquals(Math.hypot(Number.MAX_VALUE, Number.MAX_VALUE), Infinity);
assertEquals(Math.hypot(Number.MAX_VALUE, 1), Number.MAX_VALUE);
assertEquals(Math.hypot(Number.MIN_VALUE, Number.MIN_VALUE), Number.MIN_VALUE);
assertEquals(Math.hypot(1, 1, 1, 1), 2);
assertEquals(Math.hypot(1e-300, 1, 1e+300), 1e+300);

"success";
