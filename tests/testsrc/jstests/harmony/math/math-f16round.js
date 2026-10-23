load("testsrc/assert.js");

assertEquals(Math.f16round(NaN), NaN);
assertEquals(Math.f16round(0), 0);
assertEquals(Math.f16round(-0), -0);
assertEquals(Math.f16round(Infinity), Infinity);
assertEquals(Math.f16round(-Infinity), -Infinity);
assertEquals(Math.f16round(1), 1);
assertEquals(Math.f16round(-1), -1);
assertEquals(Math.f16round(1.337), 1.3369140625);
assertEquals(Math.f16round(0.1), 0.0999755859375);
assertEquals(Math.f16round(65504), 65504);
assertEquals(Math.f16round(65519.99), 65504);
assertEquals(Math.f16round(65520), Infinity);
assertEquals(Math.f16round(-65520), -Infinity);
assertEquals(Math.f16round(10000000000), Infinity);
assertEquals(Math.f16round(0.00006103515625), 0.00006103515625);
assertEquals(Math.f16round(0.000061), 0.00006097555160522461);
assertEquals(Math.f16round(0.00005), 0.000050008296966552734);
assertEquals(Math.f16round(-0.00004), -0.00003999471664428711);
assertEquals(Math.f16round(0.000030517578125), 0.000030517578125);
assertEquals(Math.f16round(5.960464477539063e-8), 5.960464477539063e-8);
assertEquals(Math.f16round(2.9802322387695312e-8), 0);
assertEquals(Math.f16round(2.980232238769532e-8), 5.960464477539063e-8);
assertEquals(Math.f16round(1.4901161193847656e-8), 0);
assertEquals(Math.f16round(1e-8), 0);
assertEquals(Math.f16round(Number.MIN_VALUE), 0);

// Inputs with a float16 exponent of 0, [2^-15, 2^-14), are subnormal in float16
assertEquals(Math.f16round(-0.000030517578125), -0.000030517578125);
assertEquals(Math.f16round(0.000030517578124999997), 0.000030517578125);
assertEquals(Math.f16round(0.00003051757812500001), 0.000030517578125);
assertEquals(Math.f16round(0.000030547380447387695), 0.000030517578125);
assertEquals(Math.f16round(0.000030606985092163086), 0.00003063678741455078);
assertEquals(Math.f16round(0.00003578066825866699), 0.000035762786865234375);
assertEquals(Math.f16round(-0.00003578066825866699), -0.000035762786865234375);

// Largest subnormal and the step to the smallest normal (2^-14)
assertEquals(Math.f16round(0.00006097555160522461), 0.00006097555160522461);
assertEquals(Math.f16round(-0.00006097555160522461), -0.00006097555160522461);
assertEquals(Math.f16round(0.000060945749282836914), 0.00006091594696044922);
assertEquals(Math.f16round(0.0000610053539276123), 0.00006097555160522461);
assertEquals(Math.f16round(0.000061005353927612305), 0.00006103515625);
assertEquals(Math.f16round(-0.000061005353927612305), -0.00006103515625);
assertEquals(Math.f16round(0.00006100535392761231), 0.00006103515625);
assertEquals(Math.f16round(0.00006103515624999999), 0.00006103515625);

// Ties to even in the lower subnormal range and at the smallest normal
assertEquals(Math.f16round(8.940696716308594e-8), 1.1920928955078125e-7);
assertEquals(Math.f16round(1.4901161193847656e-7), 1.1920928955078125e-7);
assertEquals(Math.f16round(0.0000610649585723877), 0.00006103515625);
assertEquals(Math.f16round(0.00006112456321716309), 0.00006115436553955078);

// Ties to even in the normal range
assertEquals(Math.f16round(1.00048828125), 1);
assertEquals(Math.f16round(1.00146484375), 1.001953125);
assertEquals(Math.f16round(-1.00146484375), -1.001953125);
assertEquals(Math.f16round(65519.99999999999), 65504);

"success";
