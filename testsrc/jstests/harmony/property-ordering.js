load("testsrc/assert.js");

function verifyOrder(o, a) {
  assertArrayEquals(a, Object.keys(o));
  n = [];
  for (var k in o) {
    n.push(k);
  }
  assertArrayEquals(a, n);
}

var o = {
  "z": 1,
  "b": 2,
  "d": 3,
};
o.x = 4;
verifyOrder(o, [ "z", "b", "d", "x" ]);

o = {
  10: 1,
  2: 2,
  9: 3,
};
o[3] = 4;
verifyOrder(o, [ "2", "3", "9", "10" ]);

o = {
  "z": 1,
  3: 2,
  "c": 3,
};
o.b = 4;
o[99] = 5;
verifyOrder(o, [ "3", "99", "z", "c", "b" ]);

"success";
