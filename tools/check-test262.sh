#!/bin/sh

function computeSum() {
    return $(sum $1 | awk '{print $1}')
}
export RHINO_TEST_JAVA_VERSION=11

sumBefore=$(computeSum ./tests/testsrc/test262.properties)

./gradlew :tests:test --tests Test262SuiteTest -DupdateTest262properties

sumAfter=$(computeSum ./tests/testsrc/test262.properties)
if [ "$sumBefore" != "$sumAfter" ]; then
    echo "test262.properties has not been properly updated using the built-in tools."
    echo "Please follow the instructions in tests/README.md to update it"
    echo "and include the updated file in your PR."
    exit 1
fi
