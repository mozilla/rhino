'use strict';

function assertEquals(x, y) {
  if (x !== y) {
    throw 'Expected ' + x + ' to equal ' + y;
  }
}

function makeOne() {
  return 1;
}

function makeFour() {
  return 2 + 2;
}

function makeTwo() {
  return 1 + 1;
}

function makePi() {
  return 3.14;
}

function addInts() {
  let result = makeTwo() + 2;
  assertEquals(result,  4);
  return result;
}

function addFloats() {
  let result = makePi() + 1.1;
  assertEquals(result,  4.24);
  return result;
}

function addTwoFloats() {
  let result = makePi() + makePi();
  assertEquals(result,  6.28);
  return result;
}

function subtractInts() {
  let result = 4 - makeTwo();
  assertEquals(result, 2);
  return result;
}

function subtractFloats() {
  let result = makePi() - 0.4;
  assertEquals(result, 2.74);
  return result;
}

function subtractTwoFloats() {
  let result = makePi() - makePi();
  assertEquals(result, 0);
  return result;
}

function bitwiseAnd() {
  let result = makeFour() & makeTwo();
  assertEquals(result, 0);
  return result;
}

function bitwiseOr() {
  let result = makeFour() | makeTwo();
  assertEquals(result, 6);
  return result;
}

function bitwiseLsh() {
  let result = makeTwo() << makeOne();
  assertEquals(result, 4);
  return result;
}

function bitwiseRsh() {
  let result = makeFour() >> makeOne();
  assertEquals(result, 2);
  return result;
}

function bitwiseSignedRsh() {
  let result = makeFour() >>> makeOne();
  assertEquals(result, 2);
  return result;
}

function addStringsInLoop() {
  let s = "";
  for (var i = 0; i < 10; i++) {
    s += "aa";
  }
  assertEquals(s, "aaaaaaaaaaaaaaaaaaaa");
  return s;
}

function addMixedStrings() {
  let s = "Foo " + 100 + " bars, " + true + "!";
  assertEquals(s, "Foo 100 bars, true!");
  return s;
}

addInts();
addFloats();
addTwoFloats();
addStringsInLoop();
addMixedStrings();