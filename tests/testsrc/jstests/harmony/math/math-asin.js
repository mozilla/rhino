load("testsrc/assert.js");

assertEquals(Math.asin(NaN), NaN);
assertEquals(Math.asin(0), 0);
assertEquals(Math.asin(-0), -0);
assertEquals(Math.asin(Infinity), NaN);
assertEquals(Math.asin(-Infinity), NaN);
assertEquals(Math.asin(1), 1.5707963267948966);
assertEquals(Math.asin(-1), -1.5707963267948966);
assertEquals(Math.asin(0.5), 0.5235987755982989);
assertEquals(Math.asin(-0.5), -0.5235987755982989);
assertEquals(Math.asin(1.0000000000000002), NaN);
assertEquals(Math.asin(-2), NaN);
assertEquals(Math.asin(1e-20), 1e-20);
assertEquals(Math.asin(-1e-20), -1e-20);

"success";
