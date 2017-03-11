#!/bin/sh

BUILDROOT=${BUILDROOT:-github/rhino}

(cd $BUILDROOT; ./gradlew clean test)
