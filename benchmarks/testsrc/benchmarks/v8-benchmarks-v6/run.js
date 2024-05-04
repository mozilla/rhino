// Copyright 2008 the V8 project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of Google Inc. nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


load('v8-benchmarks-v6/base.js');
load('v8-benchmarks-v6/richards.js');
load('v8-benchmarks-v6/deltablue.js');
load('v8-benchmarks-v6/crypto.js');
load('v8-benchmarks-v6/raytrace.js');
load('v8-benchmarks-v6/earley-boyer.js');
load('v8-benchmarks-v6/regexp.js');
load('v8-benchmarks-v6/splay.js');

var success = true;
var lastScore;

function printMeasurement(name, value) {
  print("<measurement><name>" + name +
      "</name><value>" + value +
      "</value></measurement>");
}

function PrintResult(name, result) {
  //printMeasurement(RUN_NAME + ' ' + name, result);
  print(name + ': ' + result);
}


function PrintError(name, error) {
  PrintResult(name, error);
  success = false;
}

function PrintScore(score) {
  lastScore = score;
  if (success) {
    print('Score (version ' + BenchmarkSuite.version + '): ' + score);
  }
}

// Run more than once to warm up JVM
var runs = 1;


for (var i = 1; i <= runs; i++) {
  BenchmarkSuite.RunSuites({ NotifyResult: PrintResult,
                             NotifyError: PrintError,
                             NotifyScore: PrintScore });
  gc();
}

lastScore;
