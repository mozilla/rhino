#!/bin/sh

BUILDROOT=${BUILDROOT:-github/rhino}

(cd $BUILDROOT; ./gradlew clean jar sourceJar javadocJar distZip)
testStatus=$?
exit ${testStatus}
