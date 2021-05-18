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

## Optimization levels
By default all tests are run 3 times, at optimization levels -1, 0 and 9.

This behavior can be changed through different means:
1. Quick disable
```
./gradlew test -Dquick
```
2. Setting an explicit optimization level through the command line:
```
./gradlew test -DTEST_OPTLEVEL=-1
```
3. Setting an explicit optimization level through the `TEST_262_OPTLEVEL` environment variable

## Running a specific TestSuite
```
./gradlew test --tests org.mozilla.javascript.tests.Test262SuiteTest 
```

## Running specific tests from the official ECMAScript Test Suite (test262)
As Rhino isn't 100% compliant with the latest ECMAScript standard, there is a mechanism to define which tests to run/skip. 
The default is [test262.properties](test262.properties).

Another .properties file to use can be specified using the `test262properties` commandline property
```
./gradlew test --tests org.mozilla.javascript.tests.Test262SuiteTest -Dtest262properties=testsrc/myOwn.properties
```