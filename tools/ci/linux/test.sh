#!/bin/sh

BUILDROOT=${BUILDROOT:-github/rhino}

(cd $BUILDROOT; git submodule init; git submodule update)
(cd $BUILDROOT; ./gradlew clean check)
testStatus=$?

for n in ${BUILDROOT}/buildGradle/test-results/*.xml
do
  bn=`basename $n .xml`
  mv $n ${BUILDROOT}/buildGradle/test-results/${bn}_sponge_log.xml
done

exit ${testStatus}

