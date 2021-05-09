load("testsrc/assert.js");

function nestedThrower(msg) {
  throw new Error(msg);
}
function parentThrower(msg) {
  nestedThrower(msg);
}
function grandparentThrower(msg) {
  parentThrower(msg);
}
function ObjectThrower(msg) {
  nestedThrower(msg);
}
function nestedCapture(o, f) {
  Error.captureStackTrace(o, f);
}
function parentCapture(o, f) {
  nestedCapture(o, f);
}
function grandParentCapture(o, f) {
  parentCapture(o, f);
}
function countLines(msg) {
  if (!msg) {
    return 0;
  }
  // Subtract one for the newline at the end
  return msg.split('\n').length - 1;
}

// Test that toString contains the error but not the stack
// and test that the stack contains the file name
try {
  throw new Error('Test 1');
} catch (e) {
  assertFalse(e.stack == undefined);
  assertTrue(/Test 1/.test(e.toString()));
  assertFalse(/stack-traces-mozilla-lf.js/.test(e.toString()));
  assertFalse(/Test 1/.test(e.stack));
  assertTrue(/stack-traces-mozilla-lf.js/.test(e.stack));
}

// Assert that the function name is nested inside a nested stack trace
try {
  nestedThrower('Nested 1');
} catch (e) {
  assertFalse(/Nested 1/.test(e.stack));
  assertTrue(/nestedThrower/.test(e.stack));
}

// Do the same for a second level of nesting
try {
  parentThrower('Nested 2');
} catch (e) {
  assertFalse(/Nested 2/.test(e.stack));
  assertTrue(/nestedThrower/.test(e.stack));
  assertTrue(/parentThrower/.test(e.stack));
}

// Do the same for a constructor
try {
  new ObjectThrower('Nested 3');
} catch (e) {
  assertFalse(/Nested 3/.test(e.stack));
  assertTrue(/nestedThrower/.test(e.stack));
  assertTrue(/ObjectThrower/.test(e.stack));
}

// Count stack lines before and after changing limit
try {
  grandparentThrower('Count 1');
} catch (e) {
  assertTrue(countLines(e.stack) >= 3);
}

assertTrue(Error.stackTraceLimit != undefined);
Error.stackTraceLimit = 2;
assertEquals(2, Error.stackTraceLimit);

try {
  grandparentThrower('Count 2');
} catch (e) {
  assertEquals(2, countLines(e.stack));
}

Error.stackTraceLimit = 0;
assertEquals(0, Error.stackTraceLimit);

try {
  grandparentThrower('Count 3');
} catch (e) {
  assertEquals(0, countLines(e.stack));
}

Error.stackTraceLimit = Infinity;
assertEquals(Infinity, Error.stackTraceLimit);

try {
  grandparentThrower('Count 1');
} catch (e) {
  assertTrue(countLines(e.stack) >= 3);
}

// Test captureStackTrace

var o = {};
grandParentCapture(o);
assertTrue(/nestedCapture/.test(o.stack));
assertTrue(/parentCapture/.test(o.stack));
assertTrue(/grandParentCapture/.test(o.stack));

// Put in a function to be hidden from the stack

var m = {};
grandParentCapture(m, parentCapture);
assertTrue(/grandParentCapture/.test(m.stack));
assertFalse(/parentCapture/.test(m.stack));
assertFalse(/nestedCapture/.test(m.stack));

// Put in a function not in the stack

var n = {};
grandParentCapture(n, print);
assertFalse(/nestedCapture/.test(n.stack));
assertFalse(/parentCapture/.test(n.stack));
assertFalse(/grandParentCapture/.test(n.stack));

// Test prepareStackTrace

assertEquals(undefined, Error.prepareStackTrace);

var prepareCalled = false;

function diagnoseStack(err, stack) {
  var s = '';
  prepareCalled = true;
  stack.forEach(function (e) {
    assertEquals('object', typeof e);
    s += e.getFunctionName() + ' ';
    s += e.getMethodName() + ' ';
    s += e.getFileName() + ' ';
    s += e.getLineNumber() + ' ';
    s += '\n';
  });
  return s;
}
Error.prepareStackTrace = diagnoseStack;

var s1 = {};
grandParentCapture(s1);
assertTrue(/nestedCapture/.test(s1.stack));
assertTrue(/parentCapture/.test(s1.stack));
assertTrue(/grandParentCapture/.test(s1.stack));
assertTrue(prepareCalled);

// Test that it is limited by the function flag as above

var s2 = {};
grandParentCapture(s2, parentCapture);
assertFalse(/nestedCapture/.test(s2.stack));
assertFalse(/parentCapture/.test(s2.stack));
assertTrue(/grandParentCapture/.test(s2.stack));

// Test that it all works on a throw. Note that the error text isn't included.

try {
  grandparentThrower('Custom 1');
} catch (e) {
  assertFalse(/Custom 1/.test(e.stack));
  assertTrue(/nestedThrower/.test(e.stack));
  assertTrue(/parentThrower/.test(e.stack));
  assertTrue(/grandparentThrower/.test(e.stack));
}

// And test that it works the old way when we format it back
Error.prepareStackTrace = undefined;

try {
  grandparentThrower('Custom 2');
} catch (e) {
  assertFalse(/Custom 2/.test(e.stack));
  assertTrue(/nestedThrower/.test(e.stack));
  assertTrue(/parentThrower/.test(e.stack));
  assertTrue(/grandparentThrower/.test(e.stack));
}

// test that all the functions on a stack frame work

function printFrame(l, f) {
  var o =
    {typeofThis: typeof f.getThis(),
     typeName: f.getTypeName(),
     function: f.getFunction(),
    functionName: f.getFunctionName(),
    methodName: f.getMethodName(),
    fileName: f.getFileName(),
    lineNumber: f.getLineNumber(),
    columnNumber: f.getColumnNumber(),
    evalOrigin: f.getEvalOrigin(),
    topLevel: f.isToplevel(),
    eval: f.isEval(),
    native: f.isNative(),
    constructor: f.isConstructor()
    };

  l.push(o);
  return l;
}

Error.prepareStackTrace = function (e, frames) {
  return frames.reduce(printFrame, []);
};

try {
  grandparentThrower('testing stack');
} catch (e) {
  e.stack.forEach(function (f) {
    verifyFrame(f);
  });
}

function verifyFrame(f) {
  assertEquals(typeof f.fileName, 'string');
  assertEquals(typeof f.lineNumber, 'number');
  assertEquals(typeof f.topLevel, 'boolean');
  assertEquals(typeof f.eval, 'boolean');
  assertEquals(typeof f.native, 'boolean');
  assertEquals(typeof f.constructor, 'boolean');
}
