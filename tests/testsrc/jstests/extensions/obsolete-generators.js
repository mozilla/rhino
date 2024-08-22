load("testsrc/assert.js");

// Basic generator
function basicGenerator(count) {
  for (var i = 0; i < count; i++) {
    yield i;
  }
}

var basic = basicGenerator(10);
for (var i = 0; i < 10; i++) {
  var result = basic.next();
  assertEquals(i, result);
}

// Try..catch
function tcGenerator(shouldThrow) {
  var gv = 0;
  try {
    gv = 1;
    yield gv;
    if (shouldThrow) {
      throw 'Throwing!';
    }
  } catch (e) {
    yield 999;
  }
  gv = 90;
  yield gv;
}

var tc = tcGenerator(false);
assertEquals(1, tc.next());
assertEquals(90, tc.next());

tc = tcGenerator(true);
assertEquals(1, tc.next());
assertEquals(999, tc.next());
assertEquals(90, tc.next());

// Try..finally
function tfGenerator() {
  var gv = 0;
  try {
    gv = 1;
    yield gv;
  } finally {
    gv = 2;
    yield gv;
  }
  gv = 90;
  yield gv;
}

var tf = tfGenerator();
assertEquals(1, tf.next());
assertEquals(2, tf.next());
assertEquals(90, tf.next());

// try..catch..finally
function tcfGenerator(shouldThrow) {
  var gv = 0;
  try {
    gv++;
    yield gv;
    if (shouldThrow) {
      throw 'Throwing!';
    }
  } catch (e) {
    gv = 10;
    yield 999;
  } finally {
    gv++;
    yield gv;
  }
  gv++;
  yield gv;
}

var tcf = tcfGenerator(false);
assertEquals(1, tcf.next());
assertEquals(2, tcf.next());
assertEquals(3, tcf.next());

tcf = tcfGenerator(true);
assertEquals(1, tcf.next());
assertEquals(999, tcf.next());
assertEquals(11, tcf.next());
assertEquals(12, tcf.next());

// nested tries
function nested(shouldThrow) {
  yield 1;
  try {
    yield 2;
    try {
      yield 3;
      if (shouldThrow) {
        yield 4;
        throw 'Nested throw!';
      }
    } catch (e) {
      yield 5;
    } finally {
      yield 6;
    }
  } finally {
    yield 7;
  }
  yield 8;
}

var nest = nested(false);
assertEquals(1, nest.next());
assertEquals(2, nest.next());
assertEquals(3, nest.next());
assertEquals(6, nest.next());
assertEquals(7, nest.next());
assertEquals(8, nest.next());

var nest = nested(true);
assertEquals(1, nest.next());
assertEquals(2, nest.next());
assertEquals(3, nest.next());
assertEquals(4, nest.next());
assertEquals(5, nest.next());
assertEquals(6, nest.next());
assertEquals(7, nest.next());
assertEquals(8, nest.next());

"success";
