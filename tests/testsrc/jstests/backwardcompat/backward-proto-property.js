load('testsrc/assert.js');

// Test old behavior of "__proto__"

// __proto__ can be set to lots of things
let obj = {};
assertTrue(obj.__proto__ === Object.prototype);
obj.__proto__ = undefined;
assertFalse(obj.__proto__ === Object.prototype);
obj.__proto__ = true;
assertFalse(obj.__proto__ === Object.prototype);
obj.__proto__ = 12345;
assertFalse(obj.__proto__ === Object.prototype);
obj.__proto__ = 'foobar';
assertFalse(obj.__proto__ === Object.prototype);

// __proto__ on an object can indeed be set to another object
let prot = {
  bar: function() { print('bar'); }
};
obj = {};
assertEquals(prot, obj.__proto__ = prot);
assertFalse(obj.__proto__ === Object.prototype);
assertEquals('function', typeof obj.__proto__.bar);
assertEquals(prot, obj.__proto__);

// __proto__ on an object can be set to null
obj = {};
assertNull(obj.__proto__ = null);
assertFalse(obj.__proto__ === Object.prototype);

//__proto__ setting on a non-object also works
function f() {}
assertTrue(f.__proto__ === Function.prototype);
f.__proto__ = prot;
assertFalse(f.__proto__ === Function.prototype);
assertTrue(f.__proto__ === prot);
f.__proto__ = null;
assertFalse(f.__proto__ === Function.prototype);

"success";