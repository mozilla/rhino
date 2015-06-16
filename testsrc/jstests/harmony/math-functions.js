load("testsrc/assert.js");

assertEquals(Math.cbrt(-1), -1);
assertEquals(Math.cbrt(0), 0);
assertEquals(Math.cbrt(1), 1);
assertEquals(Math.cbrt(2), 1.2599210498948732);

assertEquals(Math.cosh(0), 1);
assertEquals(Math.cosh(1), 1.543080634815244);
assertEquals(Math.cosh(-1), 1.543080634815244);

assertEquals(Math.expm1(-1), -0.6321205588285577);
assertEquals(Math.expm1(0), 0);
assertEquals(Math.expm1(1), 1.718281828459045);

assertEquals(Math.hypot(3, 4), 5);
assertEquals(Math.hypot(3, 4, 5), 7.0710678118654755);
assertEquals(Math.hypot(), 0);
assertEquals(Math.hypot(NaN), NaN);
assertEquals(Math.hypot(3, 4, 'foo'), NaN);
assertEquals(Math.hypot(3, 4, '5'), 7.0710678118654755);
assertEquals(Math.hypot(-3), 3);

assertEquals(Math.log1p(1), 0.6931471805599453);
assertEquals(Math.log1p(0), 0);
assertEquals(Math.log1p(-1), -Infinity);
assertEquals(Math.log1p(-2), NaN);

assertEquals(Math.log10(2), 0.3010299956639812);
assertEquals(Math.log10(1), 0);
assertEquals(Math.log10(0), -Infinity);
assertEquals(Math.log10(-2), NaN);
assertEquals(Math.log10(100000), 5);

assertEquals(Math.sinh(0), 0);
assertEquals(Math.sinh(1), 1.1752011936438014);

assertEquals(Math.tanh(0), 0);
assertEquals(Math.tanh(Infinity), 1);
assertEquals(Math.tanh(1), 0.7615941559557649);

assertEquals(Math.trunc(13.37), 13);
assertEquals(Math.trunc(42.84), 42);
assertEquals(Math.trunc(0.123), 0);
assertEquals(Math.trunc(-0.123), -0);
assertEquals(Math.trunc('-1.123'), -1);
assertEquals(Math.trunc(NaN), NaN);
assertEquals(Math.trunc('foo'), NaN);
assertEquals(Math.trunc(), NaN);

assertEquals(Math.imul(2, 4), 8);
assertEquals(Math.imul(-1, 8), -8);
assertEquals(Math.imul(-2, -2), 4);
assertEquals(Math.imul(0xffffffff, 5), -5);
assertEquals(Math.imul(0xfffffffe, 5), -10);

"success";
