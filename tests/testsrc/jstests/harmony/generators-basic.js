load("testsrc/assert.js");

function * gen() {
  for (var i = 0; i < 3; i++) {
    yield i;
   }
}


function f() {
  return 1;
}
var fr = f();
assertEquals(1, fr);

var g = gen();

// Basic yield then be done use case.

var r = g.next();
assertEquals(0, r.value);
assertEquals(false, r.done);


r = g.next();
assertEquals(1, r.value);
assertEquals(false, r.done);

r = g.next();
assertEquals(2, r.value);
assertEquals(false, r.done);

r = g.next();
assertEquals(undefined, r.value);
assertEquals(true, r.done);

r = g.next();
assertEquals(undefined, r.value);
assertEquals(true, r.done);

// Deliberately throw.

g = gen();
try {
  g.throw('This is what I threw');
  assertFalse(true);
} catch (e) {
}
r = g.next();
assertEquals(true, r.done);

// Iterate a bit before throwing.

g = gen();
r = g.next();
assertEquals(0, r.value);
assertEquals(false, r.done);

try {
  g.throw('This is what else I threw');
  assertFalse(true);
} catch (e) {
}
r = g.next();
assertEquals(true, r.done);

// Return a value from the generator.

function * genr1() {
  // Put some locals in here.
  var a = 1;
  var b = 2;
  var c = a + b;
  return "Generator return!";
}

g = genr1();
r = g.next();
assertEquals("Generator return!", r.value);
assertTrue(r.done);

function * genr2() {
  yield "I yield!";
  return "Generator return!";
}

g = genr2();
r = g.next();
assertEquals("I yield!", r.value);
assertFalse(r.done);

r = g.next();
assertEquals("Generator return!", r.value);
assertTrue(r.done);

function * genr3() {
  yield 10;
  return 20;
}

g = genr3();
r = g.next();
assertEquals(10, r.value);
assertFalse(r.done);

r = g.next();
assertEquals(20, r.value);
assertTrue(r.done);

function * genf() {
  var a = 1;
  var b = 2;
  var c = a + b;
  try {
    yield 1;
  } finally {
    yield 2;
  }
}

g = genf();
r = g.next();
assertEquals(1, r.value);
assertFalse(r.done);

r = g.next();
assertEquals(2, r.value);
assertFalse(r.done);

r = g.next();
assertEquals(undefined, r.value);
assertTrue(r.done);

function * genf1() {
  yield 1;
  try {
    yield 2;
  } finally {
    yield 3;
  }
  yield 4;
}

g = genf1();
r = g.next();
assertEquals(1, r.value);
assertFalse(r.done);

r = g.next();
assertEquals(2, r.value);
assertFalse(r.done);

r = g.next();
assertEquals(3, r.value);
assertFalse(r.done);

r = g.next();
assertEquals(4, r.value);
assertFalse(r.done);

r = g.next();
assertEquals(undefined, r.value);
assertTrue(r.done);

// toString returns the correct value
assertEquals("\nfunction* gen() {\n    for (var i = 0; i < 3; i++) {\n        yield i;\n    }\n}\n", gen.toString());

"success";
