load("testsrc/assert.js");

assertEquals(Math.clz32(64), 25);
assertEquals(Math.clz32(0), 32);

assertEquals(Math.clz32(0x00000001), 31);
assertEquals(Math.clz32(0x00000002), 30);
assertEquals(Math.clz32(0x00000004), 29);
assertEquals(Math.clz32(0x00000008), 28);
assertEquals(Math.clz32(0x000000010), 27);
assertEquals(Math.clz32(0x00000020), 26);
assertEquals(Math.clz32(0x00000040), 25);
assertEquals(Math.clz32(0x00000080), 24);
assertEquals(Math.clz32(0x00000100), 23);
assertEquals(Math.clz32(0x00000200), 22);
assertEquals(Math.clz32(0x00000400), 21);
assertEquals(Math.clz32(0x00000800), 20);
assertEquals(Math.clz32(0x00001000), 19);
assertEquals(Math.clz32(0x00002000), 18);
assertEquals(Math.clz32(0x00004000), 17);
assertEquals(Math.clz32(0x00008000), 16);
assertEquals(Math.clz32(0x00010000), 15);
assertEquals(Math.clz32(0x00020000), 14);
assertEquals(Math.clz32(0x00040000), 13);
assertEquals(Math.clz32(0x00080000), 12);
assertEquals(Math.clz32(0x00100000), 11);
assertEquals(Math.clz32(0x00200000), 10);
assertEquals(Math.clz32(0x00400000), 9);
assertEquals(Math.clz32(0x00800000), 8);
assertEquals(Math.clz32(0x01000000), 7);
assertEquals(Math.clz32(0x02000000), 6);
assertEquals(Math.clz32(0x04000000), 5);
assertEquals(Math.clz32(0x08000000), 4);
assertEquals(Math.clz32(0x10000000), 3);
assertEquals(Math.clz32(0x20000000), 2);
assertEquals(Math.clz32(0x40000000), 1);
assertEquals(Math.clz32(0x80000000), 0);
assertEquals(Math.clz32(0xFFFFFFFF), 0);
assertEquals(Math.clz32(0xFFFF0000), 0);
assertEquals(Math.clz32(0x0000FF00), 16);
assertEquals(Math.clz32(0x000000F0), 24);

assertEquals(Math.clz32(-0), 32);
assertEquals(Math.clz32(-1), 0);
assertEquals(Math.clz32(-100), 0);

assertEquals(Math.clz32(1.9), 31);
assertEquals(Math.clz32(Number.POSITIVE_INFINITY), 32);
assertEquals(Math.clz32(Number.NEGATIVE_INFINITY), 32);
assertEquals(Math.clz32(Number.NaN), 32);

assertEquals(Math.clz32(0x1_0000_0100), 23);

"success";
