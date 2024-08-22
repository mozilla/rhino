load("testsrc/assert.js");

assertThrows(function() {
  'use strict';
  return arguments.caller;
}, TypeError);

assertThrows(function() {
  'use strict';
  return arguments.callee;
}, TypeError);

assertEquals(undefined, function() {
  return arguments.caller;
}());

function f1() {
  return arguments.callee;
}

assertEquals(f1, f1());

// A simple function is not required activation.
// But when it changes to strict mode, requires activation.
assertThrows(function() {
  'use strict';
  delete Math.LN2;
}, TypeError);

"success";
