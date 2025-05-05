// Check that the `length` property of an array, which is special in
// various ways is handled correctly on when array is the prototype of
// something else with a length.

load("testsrc/assert.js");

var WithArrayPrototype = function(array) {
    this.length = array.length;
    return this;
}

var wap = WithArrayPrototype.prototype = [];

var test = new WithArrayPrototype(['abc']);

assertEquals(0, wap.length);
assertEquals(1, test.length);

'success';
