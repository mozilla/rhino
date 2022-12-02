# Rhino: JavaScript in Java

<a title="Rodrigo J De Marco, CC0, via Wikimedia Commons" href="https://commons.wikimedia.org/wiki/File:Rhino_(234581759).jpeg"><img width="384" alt="Rhino (234581759)" src="https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Rhino_%28234581759%29.jpeg/512px-Rhino_%28234581759%29.jpeg"></a>

Rhino is an implementation of JavaScript in Java.

## License

Rhino is licensed under the [MPL 2.0](./LICENSE.txt).

## Releases

The current release is <a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_14_Release">Rhino 1.7.14</a>. Please see the [Release Notes](./RELEASE-NOTES.md).

<details><summary>Releases</summary>
<table>
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

[![Mozilla](https://circleci.com/gh/mozilla/rhino.svg?style=shield)](https://app.circleci.com/pipelines/github/mozilla/rhino)

## Documentation

Information for script builders and embedders:

[Archived](http://web.archive.org/web/20210304081342/https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Documentation)

JavaDoc for all the APIs:

[https://javadoc.io/doc/org.mozilla/rhino](https://javadoc.io/doc/org.mozilla/rhino)

More resources if you get stuck:

[https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Community](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Community)

## Building

### How to Build

Rhino builds with `Gradle`. Here are some useful tasks:
```
./gradlew jar
```
Build and create `Rhino` jar in the `buildGradle/libs` directory.
```
git submodule init
git submodule update
./gradlew test
```
Build and run all the tests, including the official [ECMAScript Test Suite](https://github.com/tc39/test262).
See [Running tests](testsrc/README.md) for more detailed info about running tests.
```
./gradlew testBenchmark
```
Build and run benchmark tests.

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

## Running

Rhino can run as a stand-alone interpreter from the command line:
```
java -jar buildGradle/libs/rhino-1.7.12.jar -debug -version 200
Rhino 1.7.9 2018 03 15
js> print('Hello, World!');
Hello, World!
js>
```
There is also a "rhino" package for many Linux distributions as well as Homebrew for the Mac.

You can also embed it, as most people do. See below for more docs.

### Java 16 and later

If you are using a modular JDK that disallows the reflective access to
non-public fields (16 and later), you may need to configure the JVM with the
[`--add-opens`](https://docs.oracle.com/en/java/javase/17/migrate/migrating-jdk-8-later-jdk-releases.html#GUID-12F945EB-71D6-46AF-8C3D-D354FD0B1781)
option to authorize the packages that your scripts shall use, for example:
```
--add-opens java.desktop/javax.swing.table=ALL-UNNAMED
```

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

### Code Formatting

Code formatting was introduced in 2021. The "spotless" plugin will fail your
build if you have changed any files that have not yet been reformatted.
Please use "spotlessApply" to reformat the necessary files.

If you are the first person to touch a big file that spotless wants to make
hundreds of lines of changes to, please try to put the reformatting changes
alone into a single Git commit so that we can separate reformatting changes
from more substantive changes.

> **Warning:** If you build with Java 16 or later, you need to apply a
> workaround for a "spotless" issue. Otherwise, the task will be disabled
> and your PR may fail.
> 
> The following must be added to your `gradle.properties`.
> ```
> org.gradle.jvmargs=--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
>  --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
>  --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
>  --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
>  --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
> ```
> For more details, see https://github.com/diffplug/spotless/issues/834#issuecomment-819118761

## More Help

The Google group is the best place to go with questions:

[https://groups.google.com/forum/#!forum/mozilla-rhino](https://groups.google.com/forum/#!forum/mozilla-rhino)
