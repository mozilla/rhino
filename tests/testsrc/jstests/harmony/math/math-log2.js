load("testsrc/assert.js");

assertEquals(Math.log2(NaN), NaN);
assertEquals(Math.log2(0), -Infinity);
assertEquals(Math.log2(-0), -Infinity);
assertEquals(Math.log2(Infinity), Infinity);
assertEquals(Math.log2(-Infinity), NaN);
assertEquals(Math.log2(1), 0);
assertEquals(Math.log2(-1), NaN);
assertEquals(Math.log2(2), 1);
assertEquals(Math.log2(0.5), -1);
assertEquals(Math.log2(3), 1.584962500721156);
assertEquals(Math.log2(8), 3);
assertEquals(Math.log2(0.125), -3);
assertEquals(Math.log2(10), 3.321928094887362);
assertEquals(Math.log2(1024), 10);
assertEquals(Math.log2(536870912), 29);
assertEquals(Math.log2(9.332636185032189e-302), -1000);
assertEquals(Math.log2(2.2250738585072014e-308), -1022);
assertEquals(Math.log2(Number.MIN_VALUE), -1074);
assertEquals(Math.log2(8.98846567431158e+307), 1023);
assertEquals(Math.log2(Number.MAX_VALUE), 1024);
assertEquals(Math.log2(1e+300), 996.5784284662087);
assertEquals(Math.log2(1e-300), -996.5784284662087);

"success";
