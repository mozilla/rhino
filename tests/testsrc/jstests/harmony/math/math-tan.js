load("testsrc/assert.js");

assertEquals(Math.tan(NaN), NaN);
assertEquals(Math.tan(0), 0);
assertEquals(Math.tan(-0), -0);
assertEquals(Math.tan(Infinity), NaN);
assertEquals(Math.tan(-Infinity), NaN);
assertEquals(Math.tan(1), 1.5574077246549023);
assertEquals(Math.tan(-1), -1.5574077246549023);
assertEquals(Math.tan(0.5), 0.5463024898437905);
assertEquals(Math.tan(0.7853981633974483), 0.9999999999999999);
assertEquals(Math.tan(1e-20), 1e-20);
assertEquals(Math.tan(-1e-20), -1e-20);
assertEquals(Math.tan(3), -0.1425465430742778);
assertEquals(Math.tan(100), -0.5872139151569291);

"success";
