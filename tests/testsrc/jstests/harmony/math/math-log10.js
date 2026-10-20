load("testsrc/assert.js");

assertEquals(Math.log10(NaN), NaN);
assertEquals(Math.log10(0), -Infinity);
assertEquals(Math.log10(-0), -Infinity);
assertEquals(Math.log10(Infinity), Infinity);
assertEquals(Math.log10(-Infinity), NaN);
assertEquals(Math.log10(1), 0);
assertEquals(Math.log10(-1), NaN);
assertEquals(Math.log10(2), 0.3010299956639812);
assertEquals(Math.log10(10), 1);
assertEquals(Math.log10(100), 2);
assertEquals(Math.log10(1e-300), -300);
assertEquals(Math.log10(1e+22), 22);
assertEquals(Math.log10(1e+300), 300);
assertEquals(Math.log10(0.001), -3);
assertEquals(Math.log10(Number.MAX_VALUE), 308.25471555991675);
assertEquals(Math.log10(Number.MIN_VALUE), -323.3062153431158);
assertEquals(Math.log10(3), 0.47712125471966244);

"success";
