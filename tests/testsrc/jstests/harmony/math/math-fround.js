load("testsrc/assert.js");

assertEquals(Math.fround(NaN), NaN);
assertEquals(Math.fround(0), 0);
assertEquals(Math.fround(-0), -0);
assertEquals(Math.fround(Infinity), Infinity);
assertEquals(Math.fround(-Infinity), -Infinity);
assertEquals(Math.fround(1), 1);
assertEquals(Math.fround(-1), -1);
assertEquals(Math.fround(1.1), 1.100000023841858);
assertEquals(Math.fround(-1.1), -1.100000023841858);
assertEquals(Math.fround(0.1), 0.10000000149011612);
assertEquals(Math.fround(16777217), 16777216);
assertEquals(Math.fround(1e-50), 0);
assertEquals(Math.fround(-1e-50), -0);
assertEquals(Math.fround(1e+50), Infinity);
assertEquals(Math.fround(3.4028234663852886e+38), 3.4028234663852886e+38);
assertEquals(Math.fround(3.4028235677973366e+38), Infinity);
assertEquals(Math.fround(1.401298464324817e-45), 1.401298464324817e-45);
assertEquals(Math.fround(7e-46), 0);
assertEquals(Math.fround(7.1e-46), 1.401298464324817e-45);
assertEquals(Math.fround(Number.MIN_VALUE), 0);

"success";
