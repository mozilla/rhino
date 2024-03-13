load('testsrc/assert.js');

// Test section B.2.2.1.2 of ECMAScript 6.0, which
// defines the behavior of the "__proto__" special property.

// __proto__ can only be set to an object or to null
let obj = {};
assertTrue(obj.__proto__ === Object.prototype);
assertEquals(undefined, obj.__proto__ = undefined);
assertTrue(obj.__proto__ === Object.prototype);
assertEquals(undefined, obj.__proto__ = true);
assertTrue(obj.__proto__ === Object.prototype);
assertEquals(undefined, obj.__proto__ = 12345);
assertTrue(obj.__proto__ === Object.prototype);
assertEquals(undefined, obj.__proto__ = 'foobar');
assertTrue(obj.__proto__ === Object.prototype);

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

// However, __proto__ setting on a non-object does nothing
function f() {}
assertTrue(f.__proto__ === Function.prototype);
assertEquals(prot, f.__proto__ = prot);
assertTrue(f.__proto__ === prot);
assertEquals(null, f.__proto__ = null);
assertTrue(f.__proto__ === undefined);

// regression tests from Kanga compat-table
assertTrue({ __proto__ : [] } instanceof Array);
assertFalse({ __proto__(){} } instanceof Function)

// __proto__ on Symbol
var x = Symbol();
var y = { foo: "bar" }
assertEquals({ foo: "bar" }, x.__proto__ = y);

"success";