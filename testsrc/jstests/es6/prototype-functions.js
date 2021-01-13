/*
Check for changing the prototype of functions.
*/

load('testsrc/assert.js');

// new prototype is an object
var protoObj = { foo: function (n) { return 'foo'; } }
var target = function () {}

target.__proto__ = protoObj;
assertEquals(protoObj, target.__proto__);
assertEquals('foo', target.foo());

// new prototype is a function
protoFunc = function (n) { }

target.__proto__ = protoFunc;
assertEquals(target.__proto__, protoFunc);

"success";
