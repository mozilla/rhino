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

assertEqualsDelta(Math.atanh(1/2), 0.549306144334059, 0.0000000000001);
assertEqualsDelta(Math.atanh(0.01), 0.010000333353334763, 0.0000000000001);
assertEqualsDelta(Math.atanh(-0.2), -0.2027325540540822, 0.0000000000001);
assertEquals(Math.atanh(0), 0);
assertEquals(Math.atanh(-0), -0);
assertEquals(Math.atanh(1), Infinity);
assertEquals(Math.atanh(-1), -Infinity);
assertEquals(Math.atanh(Infinity), NaN);
assertEquals(Math.atanh(-Infinity), NaN);
assertEquals(Math.atanh(NaN), NaN);
assertEquals(Math.atanh('foo'), NaN);
assertEquals(Math.atanh(), NaN);

assertEqualsDelta(Math.asinh(1), 0.8813735870195429, 0.0000000000001);
assertEqualsDelta(Math.asinh(-1/2), -0.48121182505960336, 0.0000000000001);
assertEqualsDelta(Math.asinh(0.01), 0.009999833340832886, 0.0000000000001);
assertEquals(Math.asinh(0), 0);
assertEquals(Math.asinh(-0), -0);
assertEquals(Math.asinh(Infinity), Infinity);
assertEquals(Math.asinh(-Infinity), -Infinity);
assertEquals(Math.asinh(NaN), NaN);
assertEquals(Math.asinh('foo'), NaN);
assertEquals(Math.asinh(), NaN);

assertEquals(Math.acosh(1), 0);
assertEquals(Math.acosh(-1), NaN);
assertEqualsDelta(Math.acosh(2), 1.3169578969248166, 0.0000000000001);
assertEqualsDelta(Math.acosh(5), 2.2924316695611777, 0.0000000000001);
assertEquals(Math.acosh(0), NaN);
assertEquals(Math.acosh(-0), NaN);
assertEquals(Math.acosh(Infinity), Infinity);
assertEquals(Math.acosh(-Infinity), NaN);
assertEquals(Math.acosh(NaN), NaN);
assertEquals(Math.acosh('foo'), NaN);
assertEquals(Math.acosh(), NaN);

assertEquals(Math.log2(1), 0);
assertEquals(Math.log2(2), 1);
assertEqualsDelta(Math.log2(3), 1.584962500721156, 0.0000000000001);
assertEquals(Math.log2(0), -Infinity);
assertEquals(Math.log2(-0), -Infinity);
assertEquals(Math.log2(-2), NaN);
assertEquals(Math.log2(NaN), NaN);
assertEquals(Math.log2('foo'), NaN);
assertEquals(Math.log2(), NaN);

assertEquals(Math.sign(1), 1);
assertEquals(Math.sign(2), 1);
assertEquals(Math.sign(-3), -1);
assertEquals(Math.sign(0), 0);
assertEquals(Math.sign(-0), -0);
assertEquals(Math.sign(Infinity), 1);
assertEquals(Math.sign(-Infinity), -1);
assertEquals(Math.sign(NaN), NaN);
assertEquals(Math.sign('foo'), NaN);
assertEquals(Math.sign(), NaN);

assertEquals(Math.fround(0.5), 0.5);
assertEquals(Math.fround(5.4), 5.400000095367432);
assertEquals(Math.fround(-2.2), -2.200000047683716);
assertEquals(Math.fround(Infinity), Infinity);
assertEquals(Math.fround(-Infinity), -Infinity);
assertEquals(Math.fround(NaN), NaN);
assertEquals(Math.fround('x'), NaN);
assertEquals(Math.fround(), NaN);

"success";
