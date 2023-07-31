/*
Check for a specific bug in Rhino that makes it impossible to check the attributes
of a Symbol-keyed property on the prototype of a built-in type. For instance, the
Symbol.toStringTag property of various types.
*/

load('testsrc/assert.js');

var s = Symbol('testsym1');
assertEquals('Symbol', s[Symbol.toStringTag]);
assertFalse(s.hasOwnProperty(Symbol.toStringTag));
assertFalse(s.propertyIsEnumerable(Symbol.toStringTag));
assertTrue(Symbol.prototype.hasOwnProperty(Symbol.toStringTag));
assertFalse(Symbol.prototype.propertyIsEnumerable(Symbol.toStringTag));

"success";
