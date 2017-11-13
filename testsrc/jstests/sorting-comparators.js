load("testsrc/assert.js");

// This is basically the default algorithm specified in ECMA 22.1.3.25.1.
// We will use it to test the output
function verifyCompare(x, y, fn) {
  if (x === undefined && y === undefined) {
    return 0;
  }
  if (x === undefined) {
    return 1;
  }
  if (y === undefined) {
    return -1;
  }
  if (fn !== undefined) {
    return fn(x, y);
  }
  var xs = x.toString();
  var ys = y.toString();
  if (xs < ys) {
    return -1;
  }
  if (xs > ys) {
    return 1;
  }
  return 0;
}

function sortAndCompare(a, fn) {
  a.sort(fn);
  for (var i = 1; i < a.length; i++) {
    var c = verifyCompare(a[i - 1], a[i], fn);
    if (c > 0) {
      print(i + ": Compared " + a[i-1] + " and " + a[i] + " result = " + c);
    }
    assertTrue(c <= 0);
  }
}

sortAndCompare(["a", "b", "c"]);

sortAndCompare(["a", "b", "c"],
  function(x, y) {
    if (x < y) {
      return -1;
    }
    if (x > y) {
      return 1;
    }
    return 0;
  });


sortAndCompare(["a", "b", "c"],
  function(x, y) {
    if (x < y) {
      return 1;
    }
    if (x > y) {
      return -1;
    }
    return 0;
  });

// White-box tests devised by @sainaen and @bensummers
sortAndCompare([1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    .map(function(value, index) { return {index: index, criteria: value}; }),
    function(left, right) {
        var a = left.criteria;
        var b = right.criteria;
        if (a !== b) {
            if (a > b || a === void 0) return 1;
            if (a < b || b === void 0) return -1;
        }
        return left.index < right.index ? -1 : 1;
    });
// This one sorts fine but confuses our "verify" code.
assertDoesNotThrow(function() {
  [1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1].sort(function (a, b) { return a >= b ? 1 : -1; });
});
// weird comparator, array may not be sorted, but shouldn't fail/throw
assertDoesNotThrow(function () {
  [1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1].sort(function (a, b) { return -1; });
});
// yet another weird comparator, again shouldn't fail/throw
assertDoesNotThrow(function () {
  [1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1].sort(function (a, b) { return 1; });
});

"success";
