// In previous versions of Rhino, this code will run fine because "use strict"
// was not enforced inside a function definition.
// With ES6 mode enabled it will throw an exception on line 9.

load("testsrc/assert.js");

function strictlySetIt() {
  'use strict';
  foo = 'bar';
  assertEquals(foo, 'bar');
}

strictlySetIt();
