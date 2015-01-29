#!/bin/sh

vers=rhino1_7R5pre
pom=maven-pom.xml

mvn gpg:sign-and-deploy-file \
  -Dfile=build/${vers}/js.jar \
  -DpomFile=${pom} \
  -DrepositoryId=sonatype-nexus-staging \
  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ 

mvn gpg:sign-and-deploy-file \
  -Dfile=build/${vers}/js-sources.jar \
  -DpomFile=${pom} \
  -DrepositoryId=sonatype-nexus-staging \
  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/  \
  -Dclassifier=sources

mvn gpg:sign-and-deploy-file \
  -Dfile=build/${vers}/js-javadoc.jar \
  -DpomFile=${pom} \
  -DrepositoryId=sonatype-nexus-staging \
  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/  \
  -Dclassifier=javadoc
