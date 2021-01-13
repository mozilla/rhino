/*
Check for changing the prototype of functions.
*/

load('testsrc/assert.js');

// new prototype is an object
var protoObj = { foo: function (n) { return 'foo'; } };
var target = function () {};
var res;

res = (target.__proto__ = protoObj);
assertEquals(protoObj, target.__proto__);
assertEquals('foo', target.foo());
assertEquals(protoObj, res)


// new prototype is a function
protoFunc = function (n) { }
target = function () {};

res = (target.__proto__ = protoFunc);
assertEquals(protoFunc, target.__proto__);
assertEquals(protoFunc, res)


// new prototype is a number
target = function () {};
var proto = target.__proto__

res = (target.__proto__ = 42);
assertEquals(proto, target.__proto__);
assertEquals(42, res)


"success";
