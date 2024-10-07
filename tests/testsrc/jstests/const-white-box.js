load("testsrc/assert.js");

// These tests deliberately exercise the optimizations in ConstAwareLinker.

// Verify that things that aren't constant aren't constant
var notConstant = 1;
assertEquals(1, notConstant);
notConstant = 2;
assertEquals(2, notConstant);

// Verify that things that are constant stay constant
const constant = 1;
assertEquals(1, constant);
constant = 2;
assertEquals(1, constant);

// Verify that this works in a loop
function checkConstantness() {
  constant++;
  assertEquals(1, constant);
}
const ITERATIONS = 10;
for (let i = 0; i < ITERATIONS; i++) {
  checkConstantness();
}

// Verify that we can set a local constant in a function
function localConstantness() {
  const localConst = 1;
  assertEquals(1, localConst);
  localConst = 2;
  assertEquals(1, localConst);
}
for (let i = 0; i < ITERATIONS; i++) {
   localConstantness();
}

// Set up an object with a const field and try it out
const o = {
  notConst: 1,
};
Object.defineProperty(o, "const", {
  value: 1,
  configurable: false,
  writable: false,
});
assertEquals(1, o.notConst);
assertEquals(1, o.const);
o.notConst = 2;
o.const = 2;
assertEquals(2, o.notConst);
assertEquals(1, o.const);

// Verify that it works in a function
function objectConstness() {
  o.const++;
  assertEquals(1, o.const);
}
for (let i = 0; i < ITERATIONS; i++) {
   objectConstness();
}

'success';