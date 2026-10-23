load("testsrc/assert.js");

assertEquals(Math.asinh(NaN), NaN);
assertEquals(Math.asinh(0), 0);
assertEquals(Math.asinh(-0), -0);
assertEquals(Math.asinh(Infinity), Infinity);
assertEquals(Math.asinh(-Infinity), -Infinity);
assertEquals(Math.asinh(1e-20), 1e-20);
assertEquals(Math.asinh(-1e-20), -1e-20);
assertEquals(Math.asinh(0.5), 0.48121182505960347);
assertEquals(Math.asinh(-0.5), -0.48121182505960347);
assertEquals(Math.asinh(1), 0.881373587019543);
assertEquals(Math.asinh(-1), -0.881373587019543);
assertEquals(Math.asinh(2), 1.4436354751788103);
assertEquals(Math.asinh(-2), -1.4436354751788103);
assertEquals(Math.asinh(-100000), -12.206072645555174);
assertEquals(Math.asinh(10000000000), 23.7189981105004);
assertEquals(Math.asinh(-10000000000), -23.7189981105004);
assertEquals(Math.asinh(1e+154), 355.291251501643);
assertEquals(Math.asinh(1e+200), 461.2101657793691);
assertEquals(Math.asinh(-1e+200), -461.2101657793691);
assertEquals(Math.asinh(Number.MIN_VALUE), Number.MIN_VALUE);
assertEquals(Math.asinh(-Number.MIN_VALUE), -Number.MIN_VALUE);

"success";
