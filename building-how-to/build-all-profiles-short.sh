#!/bin/bash

set -e

mkdir .tmp

echo "############ building with 231 ################"
./gradlew clean test buildPlugin -PbuildProfile=p231
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
for i in {1..5}; do echo; done

echo "############ building with 232 ################"
./gradlew clean test buildPlugin -PbuildProfile=p232
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
for i in {1..5}; do echo; done

echo "############ building with 233 ################"
./gradlew clean test buildPlugin -PbuildProfile=p233
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
for i in {1..5}; do echo; done

echo "############ building with 241 ################"
./gradlew clean test buildPlugin -PbuildProfile=p241
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
for i in {1..5}; do echo; done

echo "############ building with 242 ################"
./gradlew clean test buildPlugin -PbuildProfile=p242
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
for i in {1..5}; do echo; done

echo "############ building with 243 ################"
./gradlew clean test buildPlugin -PbuildProfile=p243
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
