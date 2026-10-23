load("testsrc/assert.js");

assertEquals(Math.atan(NaN), NaN);
assertEquals(Math.atan(0), 0);
assertEquals(Math.atan(-0), -0);
assertEquals(Math.atan(Infinity), 1.5707963267948966);
assertEquals(Math.atan(-Infinity), -1.5707963267948966);
assertEquals(Math.atan(1), 0.7853981633974483);
assertEquals(Math.atan(-1), -0.7853981633974483);
assertEquals(Math.atan(0.5), 0.4636476090008061);
assertEquals(Math.atan(1e-20), 1e-20);
assertEquals(Math.atan(-1e-20), -1e-20);
assertEquals(Math.atan(100000000000000000000), 1.5707963267948966);
assertEquals(Math.atan(-100000000000000000000), -1.5707963267948966);
assertEquals(Math.atan(Number.MAX_VALUE), 1.5707963267948966);
assertEquals(Math.atan(Number.MIN_VALUE), Number.MIN_VALUE);

"success";
