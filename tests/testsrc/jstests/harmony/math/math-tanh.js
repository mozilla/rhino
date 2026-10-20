load("testsrc/assert.js");

assertEquals(Math.tanh(NaN), NaN);
assertEquals(Math.tanh(0), 0);
assertEquals(Math.tanh(-0), -0);
assertEquals(Math.tanh(Infinity), 1);
assertEquals(Math.tanh(-Infinity), -1);
assertEquals(Math.tanh(1), 0.7615941559557649);
assertEquals(Math.tanh(-1), -0.7615941559557649);
assertEquals(Math.tanh(0.5), 0.46211715726000974);
assertEquals(Math.tanh(1e-20), 1e-20);
assertEquals(Math.tanh(-1e-20), -1e-20);
assertEquals(Math.tanh(10), 0.9999999958776927);
assertEquals(Math.tanh(20), 1);
assertEquals(Math.tanh(40), 1);

"success";
