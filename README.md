# Rhino: JavaScript in Java

<a title="Rodrigo J De Marco, CC0, via Wikimedia Commons" href="https://commons.wikimedia.org/wiki/File:Rhino_(234581759).jpeg"><img width="384" alt="Rhino (234581759)" src="https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Rhino_%28234581759%29.jpeg/512px-Rhino_%28234581759%29.jpeg"></a>

Rhino is an implementation of JavaScript in Java.

## License

Rhino is licensed under the [MPL 2.0](./LICENSE.txt).

## Summary

Rhino requires Java 11 or higher to run, and 21 or higher to build. Java
25 is highly recommended.

To build and run a Rhino shell:

    ./gradlew run -q --console=plain

To run the tests:

    git submodule init
    git submodule update
    ./gradlew check

## Releases

The current release is <a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_9_0_Release">Rhino 1.9.0</a>. Please see the [Release Notes](./RELEASE-NOTES.md).

<details><summary>Releases</summary>
<table>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_9_0_Release">Rhino 1.9.0</a></td><td>December 22, 2025</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_8_1_Release">Rhino 1.8.1</a></td><td>December 2, 2025</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_8_0_Release">Rhino 1.8.0</a></td><td>January 2, 2025</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_15_Release">Rhino 1.7.15</a></td><td>May 3, 2024</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_14_Release">Rhino 1.7.14</a></td><td>January 6, 2022</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_13_Release">Rhino 1.7.13</a></td><td>September 2, 2020</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_12_Release">Rhino 1.7.12</a></td><td>January 13, 2020</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_11_Release">Rhino 1.7.11</a></td><td>May 30, 2019</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_10_Release">Rhino 1.7.10</a></td><td>April 9, 2018</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_9_Release">Rhino 1.7.9</a></td><td>March 15, 2018</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_8_Release">Rhino 1.7.8</a></td><td>January 22, 2018</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_7_2_Release">Rhino 1.7.7.2</a></td><td>August 24, 2017</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_7_1_RELEASE">Rhino 1.7.7.1</a></td><td>February 2, 2016</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_7_RELEASE">Rhino 1.7.7</a></td><td>June 17, 2015</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_6_RELEASE">Rhino 1.7.6</a></td><td>April 15, 2015</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7R5_RELEASE">Rhino 1.7R5</a></td><td>January 29, 2015</td></tr>
</table>
</details>


[Compatibility table](https://mozilla.github.io/rhino/compat/engines.html) which shows which advanced JavaScript
features from ES6, and ES2016+ are implemented in Rhino.

[![GitHub Action Status](https://github.com/mozilla/rhino/actions/workflows/gradle.yml/badge.svg)](https://github.com/mozilla/rhino/actions/workflows/gradle.yml)

## Documentation

Information for script builders and embedders:

[Documentation](https://rhino.github.io)

[JavaDoc](https://javadoc.io/doc/org.mozilla/rhino)

[List of projects using Rhino](USAGE.md)

## Code Structure

Rhino 1.7.15 and before were primarily used in a single JAR called "rhino.jar".

Newer releases now organize the code using Java modules. There are four primary modules and one auxiliary module for Kotlin developers:

* **rhino**: The primary codebase necessary and sufficient to run JavaScript code. Required by everything that uses Rhino. In releases *after* 1.7.15, this module does not contain the "tools" or the XML implementation.
* **rhino-tools**: Contains the shell, debugger, and the "Global" object, which many tests and other Rhino-based tools use. Note that adding Global gives Rhino the ability to print to stdout, open files, and do other things that may be considered dangerous in a sensitive environment, so it only makes sense to include if you will use it.
* **rhino-xml**: Adds the implementation of the E4X XML standard. Only required if you are using that.
* **rhino-engine**: Adds the Rhino implementation of the standard Java *ScriptEngine* interface. Some projects use this to be able to switch between script execution engines, but for anything even moderately complex it is almost always easier and always more flexible to use Rhino's API directly.
* **rhino-all**: This creates an "all-in-one" JAR that includes *rhino-runtime*, *rhino-tools*, and *rhino-xml*. This is what's used if you want to run Rhino using "java jar".

* **rhino-kotlin**: Enhanced support for code written in Kotlin, [see the details.](./rhino-kotlin/README.md)

The release contains the following other modules, which are used while building and 
testing but which are not published to Maven Central:

* **tests**: The tests that depend on all of Rhino and also the external tests, including the Mozilla legacy test scripts and the test262 tests.
* **it-android**: Integration tests for android, [see the details.](./it-android/README.md)
* **benchmarks**: Runs benchmarks using JMH.
* **examples**: Surprisingly, this contains example code.

### Recommendations

All applications that embed rhino need the main "rhino" module. Many applications don't
need anything else -- consider doing the same, for a few reasons:

* While "rhino-engine" implements the Java ScriptEngine interface, this is a strange
abstraction that does not necessarily map well to Rhino.
* "rhino-tools" includes the Global module, which many tools use because it includes
handy built-in functions like "print" and "load". However, these are not part of any
formal standard, and it includes functionality to launch programs and load
files that you may not necessarily want in your environment. (Note that "rhino" includes
an implementation of the "console" object that you may want to use instead.)

## Building

### Requirements

It's recommended to build Rhino using Java 25. However, it will build with Java 17
and up. The "spotless" tool, which enforces code formatting, will not
run on older Java versions -- it will emit a warning.

Rhino runs on Java 11 and higher. The build tools use the "--release" flag to ensure that only
features from Java 11 are used in the product.

The CI tools run the Rhino tests on Java 11, 17, 21, and 25. Regardless of what version of Java you are
building with, you can test on another Java version using the RHINO_TEST_JAVA_VERSION environment variable.

### How to Build

For normal development, you can build the code, run the static checks, and run all the tests like this:

    git submodule init
    git submodule update
    ./gradlew check

To just run the Rhino shell, you can do this from the top-level directory:

    ./gradlew run -q --console=plain

Alternately, you can build an all-in-one JAR and run that:

    ./gradlew shadowJar
    java -jar rhino-all/build/libs/rhino-all-2.0.0-SNAPSHOT.jar

And finally, you can extract the classpath and use it in a variety of ways:

    export CLASSPATH=$(./gradlew -q printClasspath)
    java org.mozilla.javascript.tools.shell.Main

### JLine-Based Console

If the JLine library is present, the Rhino shell will use it for command-line
editing. The commands above will all include JLine. However, the Gradle wrapper
interferes with JLine's ability to manipulate the terminal. For the best CLI
experience, use either of the last two options, instead of ./gradlew run.

### Benchmarking

You can also run the benchmarks:

    ./gradlew jmh

When running the benchmarks you may find a couple of environment variables useful.
* `BENCHMARK` if set will limit the benchmarks run to those matching
  the regular expression given.
* `INTERPRETED` can be set to `true` or `false` to only run the
  benchmarks in interpreted or compiled mode.
* `PROFILERS` can be set to `cpu` or `alloc` to run the async profiler
  for cpu time or memory allocations, or can be set to any other
  string which will be passed to jmh as the value of the profilers
  argument. This allows for things like running JFR as the profiler to
  collect information on lock contention or other events.

### Testing on other Java Versions

It is a good idea to test major changes on Java 11 before assuming that they will pass the CI
tests. To do this, set the environment variable RHINO_TEST_JAVA_VERSION to the version that you
want to test. For example:

    RHINO_TEST_JAVA_VERSION=11 ./gradlew check

This will only work if Gradle can find a JDK of the appropriate version. You can troubleshoot
this using the command:

    ./gradlew -q javaToolchains

Not all installers seem to put JDKs in the places where Gradle can find them. When in doubt,
installations from [Adoptium](https://adoptium.net) seem to work on most platforms.

### Testing on Android

[see here](./it-android/README.md)

### Code Coverage

The "Jacoco" coverage is enabled by default for the main published modules as well as the special 
"tests" module. Coverage is generated for each of the main projects separately and available by
running

    ./gradlew jacocoTestReport

To see an aggregated coverage report for everything, which is probably what you want, run

    ./gradlew testCodeCoverageReport

The result is in:

    ./tests/build/reports/jacoco/testCodeCoverageReport/html

## Releasing and publishing new version

1. Ensure all tests are passing
2. Remove `-SNAPSHOT` from version in `gradle.properties` in project root folder
3. Create file `gradle.properties` in `$HOME/.gradle` folder with following properties. Populate them with maven repo credentials and repo location.
```
mavenUser=
mavenPassword=
mavenSnapshotRepo=
mavenReleaseRepo=
```

4. Run `Gradle` task to publish artifacts to Maven Central.
```
./gradlew publish
```
5. Increase version and add `-SNAPSHOT` to it in `gradle.properties` in project root folder.
6. Push `gradle.properties` to `GitHub`

### Java 16 and later

If you are using a modular JDK that disallows the reflective access to
non-public fields (16 and later), you *may* need to configure the JVM with the
[`--add-opens`](https://docs.oracle.com/en/java/javase/17/migrate/migrating-jdk-8-later-jdk-releases.html#GUID-12F945EB-71D6-46AF-8C3D-D354FD0B1781)
option to authorize the packages that your scripts shall use, for example:
```
--add-opens java.desktop/javax.swing.table=ALL-UNNAMED
```

This is not necessary just to build or test Rhino -- it may be necessary when embedding it
depending on what your project does.

## Issues

Most issues are managed on GitHub:

[https://github.com/mozilla/rhino/issues](https://github.com/mozilla/rhino/issues)

## Contributing PRs

To submit a new PR, please use the following process:

* Ensure that your entire build passes "./gradlew check". This will include
code formatting and style checks and runs the tests.
* Please write tests for what you fixed, unless you can show us that existing
tests cover the changes. Use existing tests, such as those in
"testsrc/org/mozilla/javascript/tests", as a guide.
* If you fixed ECMAScript spec compatibility, take a look at test262.properties and see
if you can un-disable some tests.
* Push your change to GitHub and open a pull request.
* Please be patient as Rhino is only maintained by volunteers and we may need
some time to get back to you.
* Thank you for contributing!

## Updating Test262 tests

If you are adding new capabilities to Rhino, you may be making more test262 tests pass, which is
a good thing. Please [see the instructions](./tests/testsrc/README.md) on how to update our test262 configuration.

Because of differences between Java and JavaScript, when testing on newer Java versions, many
Unicode-related test262 tests appear to pass, but they will fail on Java 11. Please ignore these!

### Code Formatting

Code formatting was introduced in 2021. The "spotless" plugin will fail your
build if you have changed any files that have not yet been reformatted.
Please use "spotlessApply" to reformat the necessary files.

If you are the first person to touch a big file that spotless wants to make
hundreds of lines of changes to, please try to put the reformatting changes
alone into a single Git commit so that we can separate reformatting changes
from more substantive changes.

Currently, you must be building on Java 17 or higher for Spotless to run.

## More Help

GitHub is the best place to go with questions. For example, we use "GitHub discussions":

[https://github.com/mozilla/rhino/discussions](https://github.com/mozilla/rhino/discussions)
