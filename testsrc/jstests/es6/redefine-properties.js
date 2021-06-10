load("testsrc/assert.js");

// Basic property definition works
let o = {};
Object.defineProperty(o, 'foo', {value: 1, configurable: true});
assertEquals(1, o.foo);

// Redefinining from value to getter / setter works
Object.defineProperty(o, 'foo', {set: function(x) {}, get: function() { return 2; }});
assertEquals(2, o.foo);
o.foo = 3;
assertEquals(2, o.foo);

// Replacing getter and setter works
var val;
Object.defineProperty(o, 'foo', {set: function(x) { val = x; }, get: function() { return val; }});
o.foo = 4;
assertEquals(4, o.foo);
assertEquals(4, val);

// Replacing just getter leaves setter in place
Object.defineProperty(o, 'foo', {get: function() { return 999; }});
o.foo = 5;
assertEquals(999, o.foo);
assertEquals(5, val);

// Reset
Object.defineProperty(o, 'foo', {set: function(x) { val = x; }, get: function() { return val; }});
o.foo = 6;
assertEquals(6, o.foo);
assertEquals(6, val);

// Replacing just setter leaves getter in place
var val2;
Object.defineProperty(o, 'foo', {set: function(x) { val2 = x; }});
o.foo = 7;
assertEquals(6, o.foo);
assertEquals(6, val);
assertEquals(7, val2);

// Reset
Object.defineProperty(o, 'foo', {set: function(x) { val = x; }, get: function() { return val; }});
o.foo = 8;
assertEquals(8, o.foo);
assertEquals(8, val);

// Replacing getter / setter with value still works
Object.defineProperty(o, 'foo', {value: 9});
assertEquals(9, o.foo);
assertEquals(8, val);

// Replacing just setter from value removes the value completely
Object.defineProperty(o, 'foo', {set: function(x) { val = x; }});
o.foo = 10;
assertEquals(10, val);
assertEquals(undefined, o.foo);

// Replacing the getter still does not replace the setter
Object.defineProperty(o, 'foo', {get: function() { return 998; }});
o.foo = 11;
assertEquals(11, val);
assertEquals(998, o.foo);

// Reset to value
Object.defineProperty(o, 'foo', {value: 12});
assertEquals(12, o.foo);

// Replacing just getter from value makes the value un-settable
Object.defineProperty(o, 'foo', {get: function() { return 998; }});
o.foo = 12;
assertEquals(998, o.foo);

"success";