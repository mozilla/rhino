'use strict';

function createObject(name) {
  return {
    name: name,
    foo: 1,
    bar: 2,
    baz: 3,
  }
}

function createObjectFieldByField(name) {
  let o = {};
  o.name = name;
  o.foo = 1;
  o.bar = 2;
  o.baz = 3;
  return o;
}

function getName(o) {
  return o.name;
}

function check(o) {
  const x = o.foo + o.bar;
  if (x !== 3) {
    throw "Expected 3, got" + x;
  }
  return x;
}
