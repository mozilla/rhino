# Rhino: JavaScript in Java

Rhino is an implementation of JavaScript in Java.

## License

Rhino is licensed under the [MPL 2.0](./LICENSE.txt).

## Building

[![Build Status](http://ci.apigee.io/buildStatus/icon?job=Mozilla%20Rhino)](http://ci.apigee.io/job/Mozilla%20Rhino)

Rhino builds with Ant. Here are some useful tasks:

    ant jar

Build and create "js.jar" in the build/VERSION directory.

    ant junit-all

Build and run all the tests.

    ant help

to find out about the rest.

## Running

Rhino can run as a stand-alone interpreter from the command line:

    java -jar build/rhino1_7R5pre/js.jar
    Rhino 1.7 release 5 PRERELEASE 2015 01 28
    js> print('Hello, World!');
    Hello, World!
    js>

You can also embed it, as most people do. See below for more docs.

## Issues

Most issues are managed on GitHub:

[https://github.com/mozilla/rhino/issues](https://github.com/mozilla/rhino/issues)

## Additional Documentation

Additional Documentation may be found on the Mozilla site:

[https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino)

## More Help

The Google group is the best place to go with questions:

[https://groups.google.com/forum/#!forum/mozilla-rhino](https://groups.google.com/forum/#!forum/mozilla-rhino)


