#!/bin/bash

set -e

mkdir -p .tmp

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
for i in {1..5}; do echo; done

echo "############ building with 251 ################"
./gradlew clean test buildPlugin -PbuildProfile=p251
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
for i in {1..5}; do echo; done

echo "############ building with 252 ################"
./gradlew clean test buildPlugin -PbuildProfile=p252
cp build/distributions/digma-intellij-plugin*.zip ./.tmp
