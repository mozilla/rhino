load("testsrc/assert.js");

var functionCalled = false;

function testFunction() {
    functionCalled = true;
}
function callTestFunction() {
   testFunction();
}
function callCallTestFunction() {
  callTestFunction();
}

// Sanity tests
functionCalled = false;
callTestFunction();
assertTrue(functionCalled);
functionCalled = false;
callTestFunction(10);
assertTrue(functionCalled);
functionCalled = false;
callCallTestFunction();
assertTrue(functionCalled);
functionCalled = false;

// Undefined function should not parse arguments first
assertThrows(() => {
  notFound(testFunction());
});
assertFalse(functionCalled);

// Shouldn't matter if there are arguments
assertThrows(() => {
  notFound(testFunction(1, 2, 3));
});
assertFalse(functionCalled);

// Same thing should happen in a chain
assertThrows(() => {
  notFound(callTestFunction(testFunction()));
});
assertFalse(functionCalled);


// Calling a non-function should not parse arguments first
let notFunction = 'Hello, World!';
assertThrows(() => {
  notFunction(testFunction());
});
assertFalse(functionCalled);

'success';
