#!/bin/sh

mvn deploy:deploy-file \
  -Dfile=../build/rhino1.7.7/js.jar \
  -DpomFile=maven-pom.xml \
  -DrepositoryId=sonatype-nexus-snapshots \
  -Durl=https://oss.sonatype.org/content/repositories/snapshots/
