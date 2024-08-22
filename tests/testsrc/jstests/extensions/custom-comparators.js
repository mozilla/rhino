load("testsrc/assert.js");

function makeArray(length) {
  var a  = [];
  for (var i = 0; i < length; i++) {
    a.push(Math.trunc(Math.random() * 100000));
  }
  return a;
}

function ensureSorted(a) {
  var last = 0;
  for (var i in a) {
    assertTrue(a[i] >= last);
     last = a[i];
  }
}

function compareIntGood(a, b) {
  if (a < b) {
    return -1;
  }
  if (b < a) {
    return 1;
  }
  return 0;
}

function compareIntBad(a, b) {
  return (Math.random() * 10) - 5;
}

var a = makeArray(100);
a.sort(compareIntGood);
ensureSorted(a);

a = makeArray(10000);
a.sort(compareIntBad);
// Make sure that this does not throw.
// However, at this point we should not assume that the array is properly sorted!
