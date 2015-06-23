
load("testsrc/assert.js");

// Array
var r = [];
for (var n of [1, 2, 3]) {
  r.push(n*2);
}
assertEquals('2,4,6', r.join());

var r = [];
for (var n of [1, 2, 3]) {
  for (var m of [4, 5, 6]) {
    r.push(n + m);
  }
}
assertEquals('5,6,7,6,7,8,7,8,9', r.join());

var a = [0];
var r = [];
var i = 0;
for (var n of a) {
  i++;
  if (i < 3) {
    a[i] = i;
  }
  r.push(n);
}
assertEquals('0,1,2', r.join());

var a = [0, 1, 2];
var r = [];
var i = 0;
for (var n of a) {
  a.length = 0;
  r.push(n);
}
assertEquals('0', r.join());

// Array like
var a = {
  '0': 'foo',
  '1': 'bar',
  length: 2
};
var ite = Array.prototype[Symbol.iterator].call(a);
assertEquals('foo', ite.next());
assertEquals('bar', ite.next());
assertThrows(function() {
  ite.next();
}, StopIteration);

// Other
var a = {};
a[Symbol.iterator] = function() {
  return {
    n: 0,
    next: function() {
      if (this.n >= 3) {
        throw StopIteration;
      }
      return this.n++;
    }
  };
};
var r = [];
for (var n of a) {
  r.push(n);
}
assertEquals('0,1,2', r.join());

var a = [12];
Object.defineProperty(a, 0, {
  enumerable: false
});
for (var n of a) {
  assertEquals(12, n);
}

// String
var a = 'abc';
var r = '';
for (var c of a) {
  r += c.toUpperCase();
}
assertEquals('ABC', r);

// No iterable
assertThrows(function() {
  for (var n of null) {}
}, TypeError);

assertThrows(function() {
  for (var n of 0) {}
}, TypeError);

assertThrows(function() {
  var a = {};
  a[Symbol.iterator] = function() {
    return 0;
  };
  for (var n of 1) {}
}, TypeError);

// Array comprehension
assertEquals('1,4,9', [n*n for (n of [1, 2, 3])].join());
assertEquals('5,6,7,6,7,8,7,8,9', [n+m for (n of [1, 2, 3]) for (m of [4, 5, 6])].join());

// 'of' is not ECMAScript keywords.
var of = 10;
assertEquals(10, of);

(function() {
  function of() { return 12; }
  assertEquals(12, of());
})();

function f(of) { return of*2; }
assertEquals(24, f(12));

// for each-of is SyntaxError
assertThrows('for each (n of [1,2]) {}', SyntaxError);

assertThrows('[n*n for each (n of [1,2])]', SyntaxError);

assertEquals('[Symbol.iterator]', Array.prototype[Symbol.iterator].name);
assertEquals('[Symbol.iterator]', String.prototype[Symbol.iterator].name);

"success"
