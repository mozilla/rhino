load("testsrc/assert.js");

assertEquals(Math.atan2(0, 0), 0);
assertEquals(Math.atan2(-0, 0), -0);
assertEquals(Math.atan2(0, -0), 3.141592653589793);
assertEquals(Math.atan2(-0, -0), -3.141592653589793);
assertEquals(Math.atan2(1, 0), 1.5707963267948966);
assertEquals(Math.atan2(-1, 0), -1.5707963267948966);
assertEquals(Math.atan2(1, -0), 1.5707963267948966);
assertEquals(Math.atan2(0, -1), 3.141592653589793);
assertEquals(Math.atan2(-0, -1), -3.141592653589793);
assertEquals(Math.atan2(1, 1), 0.7853981633974483);
assertEquals(Math.atan2(-1, -1), -2.356194490192345);
assertEquals(Math.atan2(1, -1), 2.356194490192345);
assertEquals(Math.atan2(Infinity, Infinity), 0.7853981633974483);
assertEquals(Math.atan2(Infinity, -Infinity), 2.356194490192345);
assertEquals(Math.atan2(-Infinity, Infinity), -0.7853981633974483);
assertEquals(Math.atan2(-Infinity, -Infinity), -2.356194490192345);
assertEquals(Math.atan2(1, Infinity), 0);
assertEquals(Math.atan2(1, -Infinity), 3.141592653589793);
assertEquals(Math.atan2(-1, -Infinity), -3.141592653589793);
assertEquals(Math.atan2(Infinity, 1), 1.5707963267948966);
assertEquals(Math.atan2(NaN, 1), NaN);
assertEquals(Math.atan2(1, NaN), NaN);
assertEquals(Math.atan2(1e-300, 1e+300), 0);
assertEquals(Math.atan2(1e+300, 1e-300), 1.5707963267948966);

"success";
