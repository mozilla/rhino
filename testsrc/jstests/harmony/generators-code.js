function * genf1() {
  //yield 1;
  try {
    //yield 2;
  } finally {
    yield 3333;
  }
  yield 4444;
}

var g= genf1();
print(g.next().value);
print(g.next().value);
print(g.next().value);
