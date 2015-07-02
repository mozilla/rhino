#!/bin/sh

deployFile=`ls ../buildGradle/libs/rhino*.jar`

if [ ! -f $deployFile ]
then
  echo "File cannot be found in $deployFile"
  exit 2
fi

mvn deploy:deploy-file \
  -Dfile=${deployFile} \
  -DpomFile=maven-pom.xml \
  -DrepositoryId=sonatype-nexus-snapshots \
  -Durl=https://oss.sonatype.org/content/repositories/snapshots/
