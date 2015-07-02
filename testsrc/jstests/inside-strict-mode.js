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

"success";
