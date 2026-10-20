load("testsrc/assert.js");

assertEquals(Math.cbrt(NaN), NaN);
assertEquals(Math.cbrt(0), 0);
assertEquals(Math.cbrt(-0), -0);
assertEquals(Math.cbrt(Infinity), Infinity);
assertEquals(Math.cbrt(-Infinity), -Infinity);
assertEquals(Math.cbrt(1), 1);
assertEquals(Math.cbrt(-1), -1);
assertEquals(Math.cbrt(2), 1.2599210498948732);
assertEquals(Math.cbrt(8), 2);
assertEquals(Math.cbrt(27), 3);
assertEquals(Math.cbrt(-27), -3);
assertEquals(Math.cbrt(1000), 10);
assertEquals(Math.cbrt(1e-300), 1e-100);
assertEquals(Math.cbrt(1e+300), 1e+100);
assertEquals(Math.cbrt(Number.MAX_VALUE), 5.643803094122362e+102);
assertEquals(Math.cbrt(Number.MIN_VALUE), 1.7031839360032603e-108);
assertEquals(Math.cbrt(-Number.MIN_VALUE), -1.7031839360032603e-108);
assertEquals(Math.cbrt(0.001), 0.1);

"success";
