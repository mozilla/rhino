load("testsrc/assert.js");

function * genf1() {
  yield 1;
  try {
    yield 2;
  } finally {
    yield 3333;
  }
  yield 4444;
}

function * genf2() {
  yield 'a';
  yield 'b';
  yield 'c';
}

function * genf3() {
  yield 'aaa';
  yield * genf2();
  yield 'ccc';
}

let g = genf1();
assertEquals({value: 1, done: false}, g.next());
assertEquals({value: 2, done: false}, g.next());
assertEquals({value: 3333, done: false}, g.next());
assertEquals({value: 4444, done: false}, g.next());
assertEquals({value: undefined, done: true}, g.next());

g = genf2();
assertEquals({value: 'a', done: false}, g.next());
assertEquals({value: 'b', done: false}, g.next());
assertEquals({value: 'c', done: false}, g.next());
assertEquals({value: undefined, done: true}, g.next());

g = genf3();
assertEquals({value: 'aaa', done: false}, g.next());
assertEquals({value: 'a', done: false}, g.next());
assertEquals({value: 'b', done: false}, g.next());
assertEquals({value: 'c', done: false}, g.next());
assertEquals({value: 'ccc', done: false}, g.next());
assertEquals({value: undefined, done: true}, g.next());

"success";