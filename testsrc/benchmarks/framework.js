/*
 * This is a benchmark framework like the one that we use for
 * V8, but it's designed to be run by our JMH benchmark code in the
 * "benchmarks" top-level directory.
 */

function Benchmark(name, runCb, setupCb, teardownCb) {
  this.name = name;
  this.runCb = runCb;
  this.setupCb = setupCb;
  this.teardownCb = teardownCb;
  if (!runCb) {
    throw 'Run function must be set';
  }
}

var Benchmarks = {};

function BenchmarkSuite(name, score, benchmarks) {
  benchmarks.forEach((b) => {
    if (!b.runCb) {
      throw 'Expecting a run callback';
    }
    Benchmarks[b.name] = b;
  });
}

function setup() {
  for (var n in Benchmarks) {
    let b = Benchmarks[n];
    if (b.setupCb) {
       b.setupCb();
    }
  }
}

function cleanup() {
  for (var n in Benchmarks) {
    let b = Benchmarks[n];
    if (b.cleanupCb) {
       b.cleanupCb();
    }
  }
}

function getRunFunc(name) {
  return Benchmarks[name].runCb;
}