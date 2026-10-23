load("testsrc/assert.js");

assertEquals(Math.acos(NaN), NaN);
assertEquals(Math.acos(0), 1.5707963267948966);
assertEquals(Math.acos(-0), 1.5707963267948966);
assertEquals(Math.acos(Infinity), NaN);
assertEquals(Math.acos(-Infinity), NaN);
assertEquals(Math.acos(1), 0);
assertEquals(Math.acos(-1), 3.141592653589793);
assertEquals(Math.acos(0.5), 1.0471975511965979);
assertEquals(Math.acos(-0.5), 2.0943951023931957);
assertEquals(Math.acos(1.0000000000000002), NaN);
assertEquals(Math.acos(-1.0000000000000002), NaN);
assertEquals(Math.acos(2), NaN);
assertEquals(Math.acos(1e-20), 1.5707963267948966);

"success";
