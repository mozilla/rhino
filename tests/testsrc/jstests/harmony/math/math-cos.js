load("testsrc/assert.js");

assertEquals(Math.cos(NaN), NaN);
assertEquals(Math.cos(0), 1);
assertEquals(Math.cos(-0), 1);
assertEquals(Math.cos(Infinity), NaN);
assertEquals(Math.cos(-Infinity), NaN);
assertEquals(Math.cos(1), 0.5403023058681398);
assertEquals(Math.cos(-1), 0.5403023058681398);
assertEquals(Math.cos(0.5), 0.8775825618903728);
assertEquals(Math.cos(3.141592653589793), -1);
assertEquals(Math.cos(1e-20), 1);
assertEquals(Math.cos(3), -0.9899924966004454);
assertEquals(Math.cos(100), 0.8623188722876839);
assertEquals(Math.cos(1e+22), 0.523214785395139);
assertEquals(Math.cos(Number.MAX_VALUE), -0.9999876894265599);

"success";
