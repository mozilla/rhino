/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

/* Various tests of iterators designed to white-box-test the
 * IterableLikeIterator class. */

let returnCalled = false;
const MAX_COUNT = 3;
function makeExcellentIterator() {
  return {
    count: 0,
    next: function () {
      if (this.count < MAX_COUNT) {
        return {
          value: this.count++,
          done: false
        };
      }
      return {
        done: true
      };
    },
    return: function () {
      returnCalled = true;
    }
  }
}
let iterable = {};
iterable[Symbol.iterator] = makeExcellentIterator;

// Make sure our excellent iterator is correct.
let it = makeExcellentIterator();
let ir = it.next();
assertFalse(ir.done);
assertEquals(ir.value, 0);
ir = it.next();
assertFalse(ir.done);
assertEquals(ir.value, 1);
ir = it.next();
assertFalse(ir.done);
assertEquals(ir.value, 2);
ir = it.next();
assertTrue(ir.done);
it.return();
assertTrue(returnCalled);
let a = Array.from(iterable);
assertArrayEquals(a, [0, 1, 2]);
assertTrue(returnCalled);

// No "next" method throws TypeError
function makeNoNextIterator() {
  return {};
}
iterable[Symbol.iterator] = makeNoNextIterator;
assertThrows(function() {
  Array.from(iterable);
}, TypeError);

// No "return" method is fine
function makeNoReturnIterator() {
  return {
    count: 0,
    next: function () {
      if (this.count < MAX_COUNT) {
        return {
          value: this.count++,
          done: false
        };
      }
      return {
        done: true
      };
    }
  }
}
iterable[Symbol.iterator] = makeNoReturnIterator;
returnCalled = false;
a = Array.from(iterable);
assertArrayEquals(a, [0, 1, 2]);
assertFalse(returnCalled);

// Explicitly setting "return" to undefined is fine (a bug in previous versions)
function makeUndefinedReturnIterator() {
  return {
    count: 0,
    next: function () {
      if (this.count < MAX_COUNT) {
        return {
          value: this.count++,
          done: false
        };
      }
      return {
        done: true
      };
    },
    return: undefined
  }
}
iterable[Symbol.iterator] = makeUndefinedReturnIterator;
returnCalled = false;
a = Array.from(iterable);
assertArrayEquals(a, [0, 1, 2]);
assertFalse(returnCalled);

// Returning a non-object result throws TypeError
function makeNonObjectIterator() {
  return {
    next: function () {
      return 3;
    }
  }
}
iterable[Symbol.iterator] = makeNonObjectIterator;
assertThrows(function() {
  Array.from(iterable);
}, TypeError);

// Not defining a "done" property is the same as
// making it "false".
function makeNoDoneIterator() {
  return {
    count: 0,
    next: function () {
      if (this.count < MAX_COUNT) {
        return {
          value: this.count++
        };
      }
      return {
        done: true
      };
    },
  }
}
iterable[Symbol.iterator] = makeNoDoneIterator;
returnCalled = false;
a = Array.from(iterable);
assertArrayEquals(a, [0, 1, 2]);
assertFalse(returnCalled);

// Returning no "value" is fine
function makeNoValueIterator() {
  return {
    count: 0,
    next: function () {
      if (this.count < MAX_COUNT) {
        this.count++;
        return {
          done: false
        };
      }
      return {
        done: true
      };
    },
  }
}
iterable[Symbol.iterator] = makeNoValueIterator;
returnCalled = false;
a = Array.from(iterable);
assertArrayEquals(a, [undefined, undefined, undefined]);
assertFalse(returnCalled);

"success";
