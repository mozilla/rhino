#!/bin/sh

BUILDROOT=${BUILDROOT:-src/github/rhino}

(cd $BUILDROOT; ./gradlew clean test)
