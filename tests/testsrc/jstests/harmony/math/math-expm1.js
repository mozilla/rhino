load("testsrc/assert.js");

assertEquals(Math.expm1(NaN), NaN);
assertEquals(Math.expm1(0), 0);
assertEquals(Math.expm1(-0), -0);
assertEquals(Math.expm1(Infinity), Infinity);
assertEquals(Math.expm1(-Infinity), -1);
assertEquals(Math.expm1(-1), -0.6321205588285577);
assertEquals(Math.expm1(0.5), 0.6487212707001282);
assertEquals(Math.expm1(1e-10), 1.00000000005e-10);
assertEquals(Math.expm1(1e-20), 1e-20);
assertEquals(Math.expm1(-1e-20), -1e-20);
assertEquals(Math.expm1(10), 22025.465794806718);
assertEquals(Math.expm1(-40), -1);
assertEquals(Math.expm1(709), 8.218407461554972e+307);
assertEquals(Math.expm1(710), Infinity);

"success";
