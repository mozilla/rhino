load("testsrc/assert.js");

(function TestSymbolProperty(expected, input) {
  var arr = ['Apple', 'Banana'];

  assertFalse(arr[Symbol.iterator].toString().includes("return i;") );

  arr[Symbol.iterator] = function () { return i; };
  assertTrue(arr[Symbol.iterator].toString().includes("return i;") );
})()

"success";
