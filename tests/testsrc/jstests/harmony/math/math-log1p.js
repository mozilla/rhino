load("testsrc/assert.js");

assertEquals(Math.log1p(NaN), NaN);
assertEquals(Math.log1p(0), 0);
assertEquals(Math.log1p(-0), -0);
assertEquals(Math.log1p(Infinity), Infinity);
assertEquals(Math.log1p(-Infinity), NaN);
assertEquals(Math.log1p(1), 0.6931471805599453);
assertEquals(Math.log1p(-1), -Infinity);
assertEquals(Math.log1p(-2), NaN);
assertEquals(Math.log1p(1e-10), 9.999999999500001e-11);
assertEquals(Math.log1p(1e-20), 1e-20);
assertEquals(Math.log1p(-1e-20), -1e-20);
assertEquals(Math.log1p(-0.5), -0.6931471805599453);
assertEquals(Math.log1p(0.5), 0.4054651081081644);
assertEquals(Math.log1p(10), 2.3978952727983707);
assertEquals(Math.log1p(1e+300), 690.7755278982137);
assertEquals(Math.log1p(Number.MAX_VALUE), 709.782712893384);
assertEquals(Math.log1p(Number.MIN_VALUE), Number.MIN_VALUE);
assertEquals(Math.log1p(-0.9999999999999999), -36.7368005696771);

"success";
