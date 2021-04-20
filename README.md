# Rhino: JavaScript in Java

![Rhino](https://developer.mozilla.org/@api/deki/files/832/=Rhino.jpg)

Rhino is an implementation of JavaScript in Java.

## License

Rhino is licensed under the [MPL 2.0](./LICENSE.txt).

## Releases

<table>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7R5_RELEASE">Rhino 1.7R5</a></td><td>January 29, 2015</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_6_RELEASE">Rhino 1.7.6</a></td><td>April 15, 2015</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_7_RELEASE">Rhino 1.7.7</a></td><td>June 17, 2015</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_7_1_RELEASE">Rhino 1.7.7.1</a></td><td>February 2, 2016</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_7_2_Release">Rhino 1.7.7.2</a></td><td>August 24, 2017</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_8_Release">Rhino 1.7.8</a></td><td>January 22, 2018</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_9_Release">Rhino 1.7.9</a></td><td>March 15, 2018</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_10_Release">Rhino 1.7.10</a></td><td>April 9, 2018</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_11_Release">Rhino 1.7.11</a></td><td>May 30, 2019</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_12_Release">Rhino 1.7.12</a></td><td>January 13, 2020</td></tr>
<tr><td><a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_13_Release">Rhino 1.7.13</a></td><td>September 2, 2020</td></tr>
</table>

[Release Notes](./RELEASE-NOTES.md) for recent releases.

[Compatibility table](http://mozilla.github.io/rhino/compat/engines.html) which shows which advanced JavaScript
features from ES6, and ES2016+ are implemented in Rhino.

[![Mozilla](https://circleci.com/gh/mozilla/rhino.svg?style=shield)](https://app.circleci.com/pipelines/github/mozilla/rhino)


## Documentation

Information for script builders and embedders:

[https://developer.mozilla.org/en-US/docs/Rhino_documentation](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Documentation)

JavaDoc for all the APIs:

[http://mozilla.github.io/rhino/javadoc/index.html](http://mozilla.github.io/rhino/javadoc/index.html)

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
Build and run all the tests.
```
./gradlew test -Dquick
```
The tests in Test262Suite and MozillaSuiteTest are run 3 times by default (at differing optimization levels).
The `quick` parameter can be used to only run them once.

It is also possible to limit the Test262Suite to 1 run by setting the environment variable 
TEST_262_OPTLEVEL to for example -1.
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

## More Help

The Google group is the best place to go with questions:

[https://groups.google.com/forum/#!forum/mozilla-rhino](https://groups.google.com/forum/#!forum/mozilla-rhino)


