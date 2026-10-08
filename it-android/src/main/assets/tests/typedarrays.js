const buf = new ArrayBuffer(64);
const ia = new Int32Array(buf);
ia[0] = 1;
ia[1] = 2;
ia[2] = 3;

assertEquals(1, ia[0]);
assertEquals(2, ia[1]);
assertEquals(3, ia[2]);

'success';

