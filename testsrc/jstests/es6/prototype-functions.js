/*
Check for changing the prototype of functions.
*/

load('testsrc/assert.js');

var proto = { foo: function (n) { return 'foo'; } }
var obj = function () {}

obj.__proto__ = proto;
assertEquals(obj.__proto__, proto);
assertEquals('foo', obj.foo());

"success";
