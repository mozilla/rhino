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
</table>

[Release Notes](./RELEASE-NOTES.md) for recent releases.

[Compatability table](http://mozilla.github.io/rhino/compat/engines.html) which shows which advanced JavaScript
features from ES5, 6, and 7 are implemented in Rhino.

## Documentation

Information for script builders and embedders:

[https://developer.mozilla.org/en-US/docs/Rhino_documentation](https://developer.mozilla.org/en-US/docs/Rhino_documentation)

JavaDoc for all the APIs:

[http://mozilla.github.io/rhino/javadoc/index.html](http://mozilla.github.io/rhino/javadoc/index.html)

More resources if you get stuck:

[https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Community](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Community)

## Building

### Status of "master" branch

<table>
<tr><td><b>Java 6</b></td><td>
  <a href="http://ci.apigee.io/job/Mozilla%20Rhino%20Java%206">
    <img src="http://ci.apigee.io/buildStatus/icon?job=Mozilla%20Rhino%20Java%206"/>
  </a></td></tr>
<tr><td><b>Java 7</b></td><td>
  <a href="http://ci.apigee.io/job/Mozilla%20Rhino">
    <img src="http://ci.apigee.io/buildStatus/icon?job=Mozilla%20Rhino"/>
  </a></td></tr>
<tr><td><b>Java 8</b></td><td>
  <a href="http://ci.apigee.io/job/Mozilla%20Rhino%20Java%208">
    <img src="http://ci.apigee.io/buildStatus/icon?job=Mozilla%20Rhino%20Java%208"/>
  </a></td></tr>
</table>

### How to Build

Rhino builds with `Gradle`. Here are some useful tasks:

    ./gradlew jar

Build and create `Rhino` jar in the `build/libs` directory.

    ./gradlew test

Build and run all the tests.

    ./gradlew testBenchmark

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

    java -jar buildGradle/libs/rhino-1.7.7.1.jar
    Rhino 1.7.7 2015 05 03
    js> print('Hello, World!');
    Hello, World!
    js>

You can also embed it, as most people do. See below for more docs.

## Issues

Most issues are managed on GitHub:

[https://github.com/mozilla/rhino/issues](https://github.com/mozilla/rhino/issues)

## More Help

The Google group is the best place to go with questions:

[https://groups.google.com/forum/#!forum/mozilla-rhino](https://groups.google.com/forum/#!forum/mozilla-rhino)


