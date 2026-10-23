load("testsrc/assert.js");

assertEquals(Math.sinh(NaN), NaN);
assertEquals(Math.sinh(0), 0);
assertEquals(Math.sinh(-0), -0);
assertEquals(Math.sinh(Infinity), Infinity);
assertEquals(Math.sinh(-Infinity), -Infinity);
assertEquals(Math.sinh(1), 1.1752011936438014);
assertEquals(Math.sinh(-1), -1.1752011936438014);
assertEquals(Math.sinh(0.5), 0.5210953054937474);
assertEquals(Math.sinh(1e-20), 1e-20);
assertEquals(Math.sinh(-1e-20), -1e-20);
assertEquals(Math.sinh(10), 11013.232874703393);
assertEquals(Math.sinh(711), Infinity);
assertEquals(Math.sinh(-711), -Infinity);

"success";
