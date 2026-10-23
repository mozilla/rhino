load("testsrc/assert.js");

assertEquals(Math.log(NaN), NaN);
assertEquals(Math.log(0), -Infinity);
assertEquals(Math.log(-0), -Infinity);
assertEquals(Math.log(Infinity), Infinity);
assertEquals(Math.log(-Infinity), NaN);
assertEquals(Math.log(1), 0);
assertEquals(Math.log(-1), NaN);
assertEquals(Math.log(2), 0.6931471805599453);
assertEquals(Math.log(0.5), -0.6931471805599453);
assertEquals(Math.log(10), 2.302585092994046);
assertEquals(Math.log(100), 4.605170185988092);
assertEquals(Math.log(2.718281828459045), 1);
assertEquals(Math.log(1e-300), -690.7755278982137);
assertEquals(Math.log(1e+300), 690.7755278982137);
assertEquals(Math.log(Number.MAX_VALUE), 709.782712893384);
assertEquals(Math.log(Number.MIN_VALUE), -744.4400719213812);

"success";
