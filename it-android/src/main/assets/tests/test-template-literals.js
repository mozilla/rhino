// Warm up
for (var i = 0; i < 100; i++) {
    var s = `warm up ${i}`;
}

// Actual test
var iterations = 10000;
var start = Date.now();

for (var i = 0; i < iterations; i++) {
    var result = `Iteration ${i} with value ${i * 2}`;
}

ret = "";
var duration = Date.now() - start;
ret = "Time for " + iterations + " template literals: " + duration + "ms (avg: " + (duration/iterations).toFixed(3) + "ms) ";
// Without the fix, this would be ~10x slower on Android
if (duration > 5000) {
    ret += " FAIL";
} else {
    ret += " success"
}
ret