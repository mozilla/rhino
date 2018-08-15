/*
 * Test a particular regression in which calling __defineGetter__ on "null" would
 * result in a Java exception. Other behaviors of __defineGetter__ and _defineSetter__
 * are tested in other test suites.
 */

load("testsrc/assert.js");

// Positive case, as a sanity check
var saved;
var o = {};
__defineGetter__.call(o, 'foo', function() { return 'bar'; });
assertEquals(o.foo, 'bar');

__defineSetter__.call(o, 'foo', function(x) { saved = x; });
o.foo = 'baz';
assertEquals(saved, 'baz');
assertEquals(o.foo, 'bar');

// Negative case -- should just throw a JavaScript exception
assertThrows(function() {
  __defineGetter__.call(null, 'foo', function() {});
});

// Negative case -- should just throw a JavaScript exception
assertThrows(function() {
  __defineSetter__.call(null, 'foo', function(x) {});
});

"success";
