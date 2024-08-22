load('testsrc/assert.js');

(function() {
  // Test before clearing global (fails otherwise)
  assertEquals("[object Promise]",
      Object.prototype.toString.call(new Promise(function() {})));
})();

function defer(constructor) {
  var resolve, reject;
  var promise = new constructor((res, rej) => { resolve = res; reject = rej });
  return {
    promise: promise,
    resolve: resolve,
    reject: reject
  };
}

var asyncAssertsExpected = 0;

function assertAsyncRan() { ++asyncAssertsExpected }

function assertAsync(b, s) {
  if (b) {
    //print(s, "succeeded")
  } else {
    AbortJS(s + " FAILED!")  // Simply throwing here will have no effect.
  }
  --asyncAssertsExpected
}

function assertLater(f, name) {
  assertFalse(f()); // should not be true synchronously
  ++asyncAssertsExpected;
  var iterations = 0;
  function runAssertion() {
    if (f()) {
      //print(name, "succeeded");
      --asyncAssertsExpected;
    } else if (iterations++ < 10) {
      EnqueueMicrotask(runAssertion);
    } else {
      AbortJS(name + " FAILED!");
    }
  }
  EnqueueMicrotask(runAssertion);
}

function assertAsyncDone(iteration) {
  var iteration = iteration || 0;
  EnqueueMicrotask(function() {
    if (asyncAssertsExpected === 0)
      assertAsync(true, "all")
    else if (iteration > 10)  // Shouldn't take more.
      assertAsync(false, "all... " + asyncAssertsExpected)
    else
      assertAsyncDone(iteration + 1)
  });
}

(function() {
  var res = '';
  var f = function () { res += this; }

  Promise.resolve().then(f.bind(null));

  assertLater(function() { return res == "[object Object]"; }, "Promise.resolve().then(f.bind(null))");
})();

(function() {
  var res = '';
  var f = function () { res += this; }

  Promise.resolve().then(f.bind('abcd'));

  assertLater(function() { return res == "abcd"; }, "Promise.resolve().then(f.bind('abcd'))");
})();

(function() {
  var res = '';
  var f = function () { res += this; }

  Promise.resolve().then(() => f.bind(null).call());

  assertLater(function() { return res == "[object Object]"; }, "Promise.resolve().then(() => f.bind(null).call())");
})();

(function() {
  var res = '';
  var f = function () { res += this; }

  Promise.resolve().then(() => f.bind('abcd').call());

  assertLater(function() { return res == "abcd"; }, "Promise.resolve().then(() => f.bind('abcd').call())");
})();


assertAsyncDone()

'success';
