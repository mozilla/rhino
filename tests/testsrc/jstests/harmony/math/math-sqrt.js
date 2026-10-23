load("testsrc/assert.js");

assertEquals(Math.sqrt(NaN), NaN);
assertEquals(Math.sqrt(0), 0);
assertEquals(Math.sqrt(-0), -0);
assertEquals(Math.sqrt(Infinity), Infinity);
assertEquals(Math.sqrt(-Infinity), NaN);
assertEquals(Math.sqrt(4), 2);
assertEquals(Math.sqrt(2), 1.4142135623730951);
assertEquals(Math.sqrt(0.25), 0.5);
assertEquals(Math.sqrt(-1), NaN);
assertEquals(Math.sqrt(Number.MAX_VALUE), 1.3407807929942596e+154);
assertEquals(Math.sqrt(Number.MIN_VALUE), 2.2227587494850775e-162);
assertEquals(Math.sqrt(1e-300), 1e-150);
assertEquals(Math.sqrt(1e+300), 1e+150);

"success";
