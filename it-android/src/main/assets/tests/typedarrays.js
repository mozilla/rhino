const buf = new ArrayBuffer(64);
const ia = new Int32Array(buf);
ia[0] = 1;
ia[1] = 2;
ia[2] = 3;

assertEquals(1, ia[0]);
assertEquals(2, ia[1]);
assertEquals(3, ia[2]);

const u8 = new Uint8Array([1, 2, 3, 4]);
const copy = new Uint8Array(u8);
assertEquals(4, copy[3]);
copy.set(new Uint8Array([9, 8]), 1);
assertEquals(9, copy[1]);
assertEquals(8, copy[2]);
copy.copyWithin(0, 2);
assertEquals(8, copy[0]);
assertEquals(4, copy[1]);

'success';

