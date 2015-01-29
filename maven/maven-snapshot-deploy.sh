#!/bin/sh

mvn deploy:deploy-file \
  -Dfile=../build/rhino1_7R5pre/js.jar \
  -DpomFile=maven-pom.xml \
  -DrepositoryId=sonatype-nexus-snapshots \
  -Durl=https://oss.sonatype.org/content/repositories/snapshots/
