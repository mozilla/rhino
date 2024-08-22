# Running tests

```
./gradlew test
```
Runs the MozillaSuiteTest (and the Test262Suite if installed) 3 times (at optimization levels -1, 0 and 9)
Results can be found in `./buildGradle/reports/tests/test/index.html`

## Running the official ECMAScript Test Suite (test262)
The Rhino test source contains logic to additionally run the official [ECMAScript Test Suite](https://github.com/tc39/test262).
In order to do so, the test suite first needs to be fetched, by running the following commands:
```
git submodule init
git submodule update
```

After doing so, the `./gradlew test` command will also execute all tests that are part of the official ECMAScript Test Suite

As Rhino isn't 100% compliant with the latest ECMAScript standard, there is a mechanism to define which tests to run/skip,
through the [test262.properties](test262.properties) file, the format of which is discussed in the [test262.properties format](#test262.properties-format) section

## Optimization levels
By default all tests are run 3 times, at optimization levels -1, 0 and 9.

This behavior can be changed through different means:
1. Quick disable (will run tests with optimization level -1)
```
./gradlew test -Dquick
```
2. Setting an explicit optimization level through the command line:
```
./gradlew test -DoptLevel=9
```
3. Setting an explicit optimization level through the `TEST_262_OPTLEVEL` environment variable

## Running a specific TestSuite
```
./gradlew test --tests org.mozilla.javascript.tests.Test262SuiteTest
```

## test262.properties format
The [test262.properties](test262.properties) file is used to specify which tests from the official ECMAScript Test Suite to include/exclude,
so that the test suite can pass even though Rhino is not yet 100% standards compliant

The [test262.properties](test262.properties) file:
- lists the subfolders of the [test262](../test262) folder to include or exclude when running tests
- lists the .js test files that are expected to fail per (sub)folder

**Basics**
```
built-ins/Array <- include all the tests in the test262/built-ins/Array folder
    from/calling-from-valid-1-noStrict.js <- but expect that this tests fails
```
If `built-ins/Array/from/calling-from-valid-1-noStrict.js` indeed fails, it wont fail the test suite. If it passes, this will be logged while running the test suite

**Skipping entire folders**
```
~built-ins/decodeURI
    name.js
    S15.1.3.1_A2.4_T1.js
    S15.1.3.1_A5.2.js
```
Toplevel folders can be prefixed with a `~` to mark the folder to be skipped entirely, for example because it contains all tests for a feature not yet supported by Rhino or because running the tests take a long time.
Any files listed for a skipped folder will be skipped as well.

**Expecting all files in a (sub)folder to fail**
```
built-ins/Array <-- topLevel folder
    prototype/flatMap <-- subfolder under topLevel folder
```
If all files in a subfolder below a topLevel folderare expected to fail, instead of listing all files explicitly, just the path of the folder needs to be included under the topLevel folder

**Comments**
The test262.properties file uses the Java Properties format, with the folder/.js file paths being the property key. The value of each 'property' can be used to store a comment
```
~built-ins/Array All tests on the built-in Array class
    prototype/flatMap haven't gotten around to implementing flatMap yet
```

A Java Properties file can also have entire lines as comments, by prefixing the line with either `!` or `#`.
While the test262.properties file does support this (because it is a Java Properties file), such line comments will be lost when (re)generating the test262.properties file!

## Updating the test262.properties file
While the [test262.properties](test262.properties) file could be manually updated, the tooling also comes with a mechanism to (re)generate the file based on the current revision of the test262 submodule and the results of running Test262SuiteTest against this revision on all standard optLevels (-1, 0 & 9)

```
./gradlew test --tests org.mozilla.javascript.tests.Test262SuiteTest --rerun-tasks -DupdateTest262properties [-Dtest262properties=testsrc/myOwn.properties]
```
The .properties file generation can be parameterized to affect the output:
- rollup: include only a single line for a subfolder that contains only failing tests
- stats: include stats on the # of failed and total tests
- unsupported: include files containing tests for unsupported features

These defaults can be overridden by specifying a value for the `generateTest262properties` parameter:
- all: rollup, stats and unsupported all true (default)
- none: rollup, stats and unsupported all false
- [rollup][stats][unsupported]: the ones included will be true

Note: the tests must actually run for the .properties file to be updated. If gradle determines that nothing has changed since the last time the `test` command was run, it won't run the tests. The `--rerun-tasks` argument forces gradle to run all tests 

## Running specific tests from the official ECMAScript Test Suite (test262)
The default setup for running the test262 test suite is defined in [test262.properties](test262.properties).

Another .properties file to use can be specified using the `test262properties` commandline property
```
./gradlew test --tests org.mozilla.javascript.tests.Test262SuiteTest -Dtest262properties=testsrc/myOwn.properties
```
This allows the creation of a custom .properties file containing for example just the tests for one specific feature being implemented, which allows for (quickly) running just the tests for that specific feature