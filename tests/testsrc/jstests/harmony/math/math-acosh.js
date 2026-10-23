load("testsrc/assert.js");

assertEquals(Math.acosh(NaN), NaN);
assertEquals(Math.acosh(0), NaN);
assertEquals(Math.acosh(-0), NaN);
assertEquals(Math.acosh(Infinity), Infinity);
assertEquals(Math.acosh(-Infinity), NaN);
assertEquals(Math.acosh(1), 0);
assertEquals(Math.acosh(0.9999999999999999), NaN);
assertEquals(Math.acosh(-1), NaN);
assertEquals(Math.acosh(0.5), NaN);
assertEquals(Math.acosh(1.0000000001), 0.000014142136208675862);
assertEquals(Math.acosh(1.5), 0.9624236501192069);
assertEquals(Math.acosh(2.5), 1.566799236972411);
assertEquals(Math.acosh(10), 2.993222846126381);
assertEquals(Math.acosh(10000000000), 23.7189981105004);
assertEquals(Math.acosh(1e+154), 355.291251501643);
assertEquals(Math.acosh(2e+154), 355.98439868220294);
assertEquals(Math.acosh(1e+200), 461.2101657793691);
assertEquals(Math.acosh(-Number.MAX_VALUE), NaN);

// The correctly rounded result is 710.475860073944;
// the fdlibm algorithm (also used by V8) returns its neighbour, which the spec allows.
assertEquals(Math.acosh(Number.MAX_VALUE), 710.4758600739439);

"success";
