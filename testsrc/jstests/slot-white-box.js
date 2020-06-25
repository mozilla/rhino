load('testsrc/assert.js');

'use strict';

function TestObj() {
  this.first = 1;
  this.second = 2;
  this.third = 3;
  this.fourth = 4;
  this.fifth = 5;
  this.sixth = 6;
  this.seventh = 7;
  this.eighth = 8;
  this.ninth = 9;
  this.tenth = 10;
}

function makeAnotherObj() {
  // Fill the properties in a different order, which means that our caching
  // based on the original order won't work.
  let x = {};
  x.tenth = 10;
  x.ninth = 9;
  x.eighth = 8;
  x.seventh = 7;
  x.sixth = 6;
  x.fifth = 5;
  x.fourth = 4;
  x.third = 3;
  x.second = 2;
  x.first = 1;
  return x;
}

// Test that repeated property access works both for "index" and regular
// property access. Use more than eight slots to test both kinds of properties.
function checkAllProperties(o) {
  assertEquals(1, o.first);
  assertEquals(2, o.second);
  assertEquals(10, o.tenth);
  assertEquals(1, o['first']);
  assertEquals(2, o['second']);
  assertEquals(3, o['third']);

  ['first', 'second', 'third', 'tenth'].forEach((pn) => {
    if (pn === 'first') {
      assertEquals(1, o[pn]);
    } else if (pn === 'second') {
      assertEquals(2, o[pn]);
    } else if (pn === 'third') {
      assertEquals(3, o[pn]);
    } else if (pn === 'tenth') {
      assertEquals(10, o[pn]);
    }
  });
}

let o1 = new TestObj();
for (let i = 0; i < 10; i++) {
  checkAllProperties(o1);
  checkAllProperties(new TestObj());
  // Test with an object that does not obey the same rules
  checkAllProperties(makeAnotherObj());
}

// Test that deleted slots stay deleted and that we can re-create them
function checkDelete(o) {
  delete(o.fourth);
  assertEquals(undefined, o.fourth);
  o.fourth = 123;
  assertEquals(123, o.fourth);
}

let o2 = new TestObj();
for (let i = 0; i < 10; i++) {
  checkDelete(o2);
  checkDelete(new TestObj());
  checkDelete(makeAnotherObj());
}

// Test that we can replace a slot with a getter and then unreplace it
function defineProperties(o) {
  Object.defineProperty(o, 'second', {
    configurable: true,
    value: 111
  });
  assertEquals(111, o.second);

  var val = 2;
  Object.defineProperty(o, 'second', {
    configurable: true,
    set: function(x) { val = x; },
    get: function() { return val; }
  });
  assertEquals(2, o.second);
  o.second = 23;
  assertEquals(23, o.second);

  Object.defineProperty(o, 'second', {
      value: 999
  });
  assertEquals(999, o.second);
  o.second = 1;
  assertEquals(1, o.second);
}

let o3 = new TestObj();
for (let i = 0; i < 10; i++) {
  defineProperties(o3);
  defineProperties(new TestObj());
}

"success";
