load("testsrc/assert.js");

// Check basic property descriptors

assertTrue(Symbol.iterator !== undefined);
var desc = Object.getOwnPropertyDescriptor(Symbol, "iterator");
assertFalse(desc.writable);
assertFalse(desc.configurable);
assertFalse(desc.enumerable);

assertTrue(Symbol.toStringTag !== undefined);
desc = Object.getOwnPropertyDescriptor(Symbol, "toStringTag");
assertFalse(desc.writable);
assertFalse(desc.configurable);
assertFalse(desc.enumerable);

assertTrue(Symbol.species !== undefined);
desc = Object.getOwnPropertyDescriptor(Symbol, "species");
assertFalse(desc.writable);
assertFalse(desc.configurable);
assertFalse(desc.enumerable);

// Check existence of Symbol stuff

assertEquals("symbol", typeof Symbol());
assertEquals("symbol", typeof Symbol("qwe"));

// Create symbols and ensure that they are not the same

var s1 = Symbol("sym");
var s2 = Symbol("sym");
var su = Symbol();
assertFalse(s1 == s2);
assertFalse(s1 === s2);

// Check toString

assertEquals("Symbol(sym)", s1.toString());
assertEquals("Symbol()", su.toString());

assertEquals("Symbol", Symbol.prototype[Symbol.toStringTag]);

// Check the global symbol table

var g1 = Symbol.for("global");
var g2 = Symbol.for("global");
assertEquals(g1, g2);
assertSame(g1, g2);
var k1 = Symbol.keyFor(g1);
assertEquals("global", k1);
var ku = Symbol.keyFor(s1);
assertEquals(undefined, ku);

// Use symbols as property identifiers.
// They should work as elements but not as property names.

var obj = {};
obj.foo = 'foo';
obj['bar'] = 'bar';
obj[123] = 'baz';
obj[s1] = 's1';

assertEquals('foo', obj.foo);
assertEquals('foo', obj['foo']);
assertEquals('bar', obj.bar);
assertEquals('bar', obj['bar']);
assertEquals('baz', obj[123]);
assertEquals('s1', obj[s1]);
assertEquals(undefined, obj.s1);

// Use symbols as function identifiers

var callMeSym = Symbol("callMe");

function callMe() {
  return "maybe";
}

function callMeTwice() {
  return "maybemaybe";
}

obj.callMe = callMe;
obj[callMeSym] = callMeTwice;
assertEquals("maybe", obj.callMe());
assertEquals("maybemaybe", obj[callMeSym]());

// Symbol prototype 
assertEquals("object", typeof Symbol.prototype);

Symbol.prototype.myCustomFunction = function() {};
assertEquals("function", typeof Symbol.prototype.myCustomFunction);


"success";
