load("testsrc/assert.js");

assertEquals(Math.sin(NaN), NaN);
assertEquals(Math.sin(0), 0);
assertEquals(Math.sin(-0), -0);
assertEquals(Math.sin(Infinity), NaN);
assertEquals(Math.sin(-Infinity), NaN);
assertEquals(Math.sin(1), 0.8414709848078965);
assertEquals(Math.sin(-1), -0.8414709848078965);
assertEquals(Math.sin(0.5), 0.479425538604203);
assertEquals(Math.sin(3.141592653589793), 1.2246467991473532e-16);
assertEquals(Math.sin(1e-20), 1e-20);
assertEquals(Math.sin(-1e-20), -1e-20);
assertEquals(Math.sin(3), 0.1411200080598672);
assertEquals(Math.sin(100), -0.5063656411097588);
assertEquals(Math.sin(1e+22), -0.8522008497671888);
assertEquals(Math.sin(Number.MAX_VALUE), 0.004961954789184062);

"success";
