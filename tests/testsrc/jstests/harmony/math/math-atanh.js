load("testsrc/assert.js");

assertEquals(Math.atanh(NaN), NaN);
assertEquals(Math.atanh(0), 0);
assertEquals(Math.atanh(-0), -0);
assertEquals(Math.atanh(Infinity), NaN);
assertEquals(Math.atanh(-Infinity), NaN);
assertEquals(Math.atanh(1), Infinity);
assertEquals(Math.atanh(-1), -Infinity);
assertEquals(Math.atanh(0.25), 0.25541281188299536);
assertEquals(Math.atanh(1e-10), 1e-10);
assertEquals(Math.atanh(1e-20), 1e-20);
assertEquals(Math.atanh(-1e-20), -1e-20);
assertEquals(Math.atanh(-1e-300), -1e-300);
assertEquals(Math.atanh(Number.MIN_VALUE), Number.MIN_VALUE);
assertEquals(Math.atanh(-Number.MIN_VALUE), -Number.MIN_VALUE);
assertEquals(Math.atanh(0.9999999999999999), 18.714973875118524);
assertEquals(Math.atanh(-0.9999999999999999), -18.714973875118524);
assertEquals(Math.atanh(1.0000000000000002), NaN);
assertEquals(Math.atanh(2), NaN);
assertEquals(Math.atanh(-2), NaN);

"success";
