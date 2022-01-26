#!/usr/bin/env bash

RELEASE_LEVEL=$1
MODULE_NAME=${PWD##*/}

echo "Release \"$MODULE_NAME\" with level '$RELEASE_LEVEL'"
tag=$(clj -Mrelease $RELEASE_LEVEL --spit --output-dir src --formats clj,json --namespace ${MODULE_NAME}.meta)

if [ $? -eq 0 ]; then
    echo "Successfully released \"$MODULE_NAME\" to $tag"
else
    echo "Fail to release \"$MODULE_NAME\"!"
    exit $?
fi


####################################################
#                                                  #
#     Clojars uploading stuff (easier with Maven)  #
#                                                  #
####################################################
ARTIFACT_ID=$(cat src/${MODULE_NAME}/meta.json | jq -r '."module-name"')
ARTIFACT_VERSION=$(cat src/${MODULE_NAME}/meta.json | jq -r '."version"')
ARTIFACT_TAG=$(cat src/${MODULE_NAME}/meta.json | jq -r '."tag"')
JAR_FILENAME="$ARTIFACT_ID-$ARTIFACT_VERSION.jar"

if [[ $ARTIFACT_TAG =~ v(.+) ]]; then
    newversion=${BASH_REMATCH[1]}
else
    echo "unable to parse tag $tag"
    exit 1
fi

if [ $? -eq 0 ]; then
    echo "Successfully set new version of \"$MODULE_NAME\"'s pom to $newversion"
else
    echo "Fail to set new version of \"$MODULE_NAME\"'s pom!"
    exit $?
fi

####################################################
# build jar                                        #
####################################################
source ./build.sh

# mvn deploy 2>&1 > /dev/null

mvn org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file \
    -Durl=https://clojars.org/repo \
    -DrepositoryId=clojars \
    -Dfile=target/$JAR_FILENAME \
    -DpomFile=pom.xml \
    -Dclassifier=

if [ $? -eq 0 ]; then
    echo "Successfully deployed \"$MODULE_NAME\" version $newversion to clojars"
else
    echo "Fail to deploy \"$MODULE_NAME\" to clojars!"
    exit $?
fi
