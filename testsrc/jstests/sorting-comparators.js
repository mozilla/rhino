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

// Torture tests devised by @sainen and @bensummers
// array's length has to be more than 17 to get into |hybridSort()|
// |0| is in the middle to be chosen as pivot on the first |partition()| call
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


sortAndCompare(["UHl3DzELNleHuQ", "Na6de_6oK1SkkNPRHLib8Y2Lwg",
                   "GnPrJj5R7savc5YGqRQ8Vf5z", "AmV1H9UWHU1GxQ",
                   "i7w_JpplQauf2jAD8ko", "z-qs4duUdshTy686Pg4",
                   "EI8kRBMNjZfRC9YH-Q", "w-A", "ukUm_NcvcToZyKn4Vw",
                   "05C4qwB52eGjZcf83_YO3PSbYA", "_73TGwD1CRtoWdwdCTZdw8E",
                   "NnXYyCqBJ-q036o", "eg", "uUIuxNDHfoHgntjZnqrvrjjTVJc",
                   "bhY", "qSzGSQ", "LpLx6ZJDp0LNHZsoRee3wYim", "uDXkQA", "hhWgHw"]
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

"success";