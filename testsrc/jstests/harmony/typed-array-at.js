load("testsrc/assert.js");
const int8 = new Int8Array([0, 10, -10, 20, -30, 40, -50]);
assertEquals(0, int8.at(0))
assertEquals(-50, int8.at(-1))
assertEquals(-10, int8.at(2))
assertEquals(undefined, int8.at(Infinity))
assertEquals(undefined, int8.at(11))
assertEquals(0,int8.at("a"));
assertEquals(0,int8.at({}));
assertEquals(0,int8.at(NaN));
assertEquals(0,int8.at(undefined));

const int16 = new Int16Array([0x1FFFA, 0x7FFA - 0x8000,0x1FFFF]);
assertEquals(-6,int16.at(1));
assertEquals(-1,int16.at(-1));

const u8clamped = new Uint8ClampedArray([5,10]);
assertEquals(10,u8clamped.at(-1));
assertEquals(10,u8clamped.at(1));
assertEquals(5,u8clamped.at(0));

const float32 = new Float32Array([1.2123410, 12.3223]);
assertEquals(float32[0],float32.at(0))
assertEquals(float32[1],float32.at(-1))
"success"
