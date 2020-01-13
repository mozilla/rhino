#!/bin/sh

function deploy {
  if [ ! -f $1 ]
  then
    echo "Missing $1"
    exit 1
  fi

  tf=/var/tmp/file.$$.jar
  rm -f ${tf}
  cp $1 ${tf}

  mvn gpg:sign-and-deploy-file \
  -Dfile=${tf} \
  -DpomFile=${2} \
  -DrepositoryId=sonatype-nexus-staging \
  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
  -Dclassifier=${3}
 
  rm ${tf}
}

vers=`egrep '^version=' ../gradle.properties | awk -F = '{print $2}'`
base=${HOME}/.m2/repository/org/mozilla

echo "Deploying ${vers}"

rb=${base}/rhino/${vers}
deploy ${rb}/rhino-${vers}.jar maven-pom.xml
deploy ${rb}/rhino-${vers}-sources.jar maven-pom.xml sources
deploy ${rb}/rhino-${vers}-javadoc.jar maven-pom.xml javadoc

rb=${base}/rhino-runtime/${vers}
deploy ${rb}/rhino-runtime-${vers}.jar maven-runtime-pom.xml
deploy ${rb}/rhino-runtime-${vers}-sources.jar maven-runtime-pom.xml sources
deploy ${rb}/rhino-runtime-${vers}-javadoc.jar maven-runtime-pom.xml javadoc