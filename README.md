# Rhino: JavaScript in Java

<a title="Rodrigo J De Marco, CC0, via Wikimedia Commons" href="https://commons.wikimedia.org/wiki/File:Rhino_(234581759).jpeg"><img width="384" alt="Rhino (234581759)" src="https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Rhino_%28234581759%29.jpeg/512px-Rhino_%28234581759%29.jpeg"></a>

Rhino is an implementation of JavaScript in Java.

## License

Rhino is licensed under the [MPL 2.0](./LICENSE.txt).

## Releases

The current release is <a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_15_Release">Rhino 1.7.15</a>. Please see the [Release Notes](./RELEASE-NOTES.md).

<details><summary>Releases</summary>
<table>
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

[Archived](http://web.archive.org/web/20210304081342/https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Documentation)

JavaDoc for all the APIs:

[https://javadoc.io/doc/org.mozilla/rhino](https://javadoc.io/doc/org.mozilla/rhino)

## Code Structure

Rhino 1.7.15 and before were primarily used in a single JAR called "rhino.jar".

Newer releases now organize the code using Java modules. There are four primary modules:

* **rhino**: The primary codebase necessary and sufficient to run JavaScript code. Required by everything that uses Rhino. In releases *after* 1.7.15, this module does not contain the "tools" or the XML implementation.
* **rhino-tools**: Contains the shell, debugger, and the "Global" object, which many tests and other Rhino-based tools use. Note that adding Global gives Rhino the ability to print to stdout, open files, and do other things that may be considered dangerous in a sensitive environment, so it only makes sense to include if you will use it.
* **rhino-xml**: Adds the implementation of the E4X XML standard. Only required if you are using that.
* **rhino-engine**: Adds the Rhino implementation of the standard Java *ScriptEngine* interface. Some projects use this to be able to switch between script execution engines, but for anything even moderately complex it is almost always easier and always more flexible to use Rhino's API directly.

The release contains the following other modules, which are used while building and 
testing but which are not published to Maven Central:

* **rhino-all**: This creates an "all-in-one" JAR that includes *rhino-runtime*, *rhino-tools*, and *rhino-xml*. This is what's used if you want to run Rhino using "java jar".
* **tests**: The tests that depend on all of Rhino and also the external tests, including the Mozilla legacy test scripts and the test262 tests.
* **benchmarks**: Runs benchmarks using JMH.
* **examples**: Surprisingly, this contains example code.

## Building

### Requirements

Rhino requires Java 17 or higher to build. The "spotless" tool, which enforces code formatting, will not
run on older Java versions and you will receive a warning. If in doubt, Java 21 works great.

Rhino runs on Java 11 and higher. The build tools use the "--release" flag to ensure that only
features from Java 11 are used in the product.

The CI tools run the Rhino tests on Java 11, 17, and 21. Regardless of what version of Java you are
building with, you can test on another Java version using the RHINO_TEST_JAVA_VERSION environment variable.

### How to Build

For normal development, you can build the code, run the static checks, and run all the tests like this:

    git submodule init
    git submodule update
    ./gradlew check

To just run the Rhino shell, you can do this from the top-level directory:

    ./gradlew run -q --console=plain

Alternately, you can build an all-in-one JAR and run that:

    ./gradlew :rhino-all:build
    java -jar rhino-all/build/libs/rhino-all-1.7.16-SNAPSHOT.jar

You can also run the benchmarks:

    ./gradlew jmh

### Testing on other Java Versions

It is a good idea to test major changes on Java 11 before assuming that they will pass the CI
tests. To do this, set the environment variable RHINO_TEST_JAVA_VERSION to the version that you
want to test. For example:

    RHINO_TEST_JAVA_VERSION=11 ./gradlew check

This will only work if Gradle can find a JDK of the appropriate version. You can troubleshoot
this using the command:

    ./gradlew -q javaToolchains

Not all installers seem to put JDKs in the places where Gradle can find them. When in doubt,
installatioons from [Adoptium](https://adoptium.net) seem to work on most platforms.

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
