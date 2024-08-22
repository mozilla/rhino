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


// new prototype is a string
target = function () {};
var protoStr = "hello";
proto = target.__proto__

res = (target.__proto__ = protoStr);
assertEquals(proto, target.__proto__);
assertEquals(protoStr, res)


// new prototype is a Symbol
target = function () {};
var protoSym = Symbol();
proto = target.__proto__

res = (target.__proto__ = protoSym);
assertEquals(proto, target.__proto__);
assertEquals(protoSym, res)


// new prototype is undefined
target = function () {};
proto = target.__proto__

res = (target.__proto__ = undefined);
assertEquals(proto, target.__proto__);
assertEquals(undefined, res)


// new prototype is null
target = function () {};
proto = target.__proto__

res = (target.__proto__ = null);
assertEquals(undefined, target.__proto__);
assertEquals(null, res)


"success";
