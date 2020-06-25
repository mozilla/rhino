function TestObj() {
  this.first = 1;
  this.second = 2;
  this.third = 3;
  this.string = 'Hello';
  this.excellent = true;
}

let o = new TestObj();

for (var i = 0; i < 1000; i++) {
  if (o.first !== 1) {
    throw 'Wrong value';
  }
  if (o.second !== 2) {
    throw 'Wrong value';
  }
  if (o.third !== 3) {
    throw 'Wrong value';
  }
  if (o.string !== 'Hello') {
    throw 'Wrong value';
  }
  if (!o.excellent) {
    throw 'Bogus';
  }
}

o;
