load("testsrc/assert.js");

assertEquals(Math.cosh(NaN), NaN);
assertEquals(Math.cosh(0), 1);
assertEquals(Math.cosh(-0), 1);
assertEquals(Math.cosh(Infinity), Infinity);
assertEquals(Math.cosh(-Infinity), Infinity);
assertEquals(Math.cosh(0.5), 1.1276259652063807);
assertEquals(Math.cosh(1e-20), 1);
assertEquals(Math.cosh(20), 242582597.70489514);
assertEquals(Math.cosh(711), Infinity);
assertEquals(Math.cosh(-711), Infinity);

"success";
