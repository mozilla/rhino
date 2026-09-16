load("testsrc/assert.js");

let _currentCounter = 0;

const counter = {};

Object.defineProperty(counter, "count", {
  get() {
    return ++_currentCounter;
  }
});

function dotNamed() {
  return counter.count;
}

const dotArrow = () => counter.count;

function index() {
  return counter["count"];
}

assertEquals(1, counter.count);
assertEquals(2, counter.count);
assertEquals(3, counter.count);

assertEquals(4, dotNamed());
assertEquals(5, dotNamed());
assertEquals(6, dotNamed());

assertEquals(7, dotArrow());
assertEquals(8, dotArrow());
assertEquals(9, dotArrow());

assertEquals(10, index());
assertEquals(11, index());
assertEquals(12, index());

"success";