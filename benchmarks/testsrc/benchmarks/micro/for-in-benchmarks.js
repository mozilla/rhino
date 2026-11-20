'use strict';

function countKeys(o) {
  let n = 0;
  for (const k in o) {
    n++;
  }
  return n;
}

// Own enumerable properties only; the prototype chain has none.
var plainObject = { a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7, h: 8 };

// ES5-style instance: own fields plus enumerable methods on the prototype.
function Point(x, y) {
  this.x = x;
  this.y = y;
  this.z = 0;
  this.w = 1;
}
Point.prototype.add = function (p) { return new Point(this.x + p.x, this.y + p.y); };
Point.prototype.scale = function (f) { return new Point(this.x * f, this.y * f); };
Point.prototype.length = function () { return Math.sqrt(this.x * this.x + this.y * this.y); };
Point.prototype.toString = function () { return this.x + ',' + this.y; };
var instance = new Point(1, 2);

// Three levels with enumerable properties, one shadowed by a non-enumerable one.
var base = { a: 1, b: 2, c: 3 };
var middle = Object.create(base, {
  d: { value: 4, enumerable: true },
  e: { value: 5, enumerable: true },
  a: { value: 6, enumerable: false },
});
var chain = Object.create(middle);
chain.f = 7;
chain.g = 8;

var array = [];
for (let i = 0; i < 100; i++) {
  array.push(i);
}

function forInPlainObject() { return countKeys(plainObject); }
function forInInstance() { return countKeys(instance); }
function forInChain() { return countKeys(chain); }
function forInArray() { return countKeys(array); }
