load("testsrc/assert.js");

assertEquals(Math.exp(NaN), NaN);
assertEquals(Math.exp(0), 1);
assertEquals(Math.exp(-0), 1);
assertEquals(Math.exp(Infinity), Infinity);
assertEquals(Math.exp(-Infinity), 0);
assertEquals(Math.exp(1), 2.718281828459045);
assertEquals(Math.exp(-1), 0.36787944117144233);
assertEquals(Math.exp(0.5), 1.6487212707001282);
assertEquals(Math.exp(1e-20), 1);
assertEquals(Math.exp(10), 22026.465794806718);
assertEquals(Math.exp(100), 2.6881171418161356e+43);
assertEquals(Math.exp(709), 8.218407461554972e+307);
assertEquals(Math.exp(710), Infinity);
assertEquals(Math.exp(-745), Number.MIN_VALUE);
assertEquals(Math.exp(-746), 0);

"success";
