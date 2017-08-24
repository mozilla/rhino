#!/bin/sh

BUILDROOT=${BUILDROOT:-github/rhino}

(cd $BUILDROOT; ./gradlew clean jar sourceJar rhinoJavadocJar distZip)
testStatus=$?
exit ${testStatus}
