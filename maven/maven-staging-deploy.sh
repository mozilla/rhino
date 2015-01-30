#!/bin/sh

vers=`egrep '^version:' ../build.properties | awk '{print $2}'`

echo "Deploying ${vers}"

pom=maven-pom.xml
jsjar=../build/rhino${vers}/js.jar
srczip=../rhino${vers}-sources.zip
doczip=../build/rhino${vers}/javadoc.zip

if [ ! -f $jsjar ]
then
  echo "Missing js.jar"
  exit 1
fi

if [ ! -f $srczip ]
then
  echo "Missing rhino${vers}-sources.zip. Run \"ant source-zip\"."
  exit 2
fi

if [ ! -f $doczip ]
then
  echo "Missing javadoc.zip. Run \"ant javadoc\"."
  exit 3
fi

mvn gpg:sign-and-deploy-file \
  -Dfile=${jsjar} \
  -DpomFile=${pom} \
  -DrepositoryId=sonatype-nexus-staging \
  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ 

mvn gpg:sign-and-deploy-file \
  -Dfile=${srczip} \
  -DpomFile=${pom} \
  -DrepositoryId=sonatype-nexus-staging \
  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/  \
  -Dclassifier=sources

mvn gpg:sign-and-deploy-file \
  -Dfile=${doczip} \
  -DpomFile=${pom} \
  -DrepositoryId=sonatype-nexus-staging \
  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/  \
  -Dclassifier=javadoc
