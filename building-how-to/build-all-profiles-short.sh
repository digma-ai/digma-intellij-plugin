#!/bin/bash

set -e


echo "############ building with 231 ################"
./gradlew clean buildPlugin -PbuildProfile=p231
cp build/distributions/digma-intellij-plugin*.zip .
for i in {1..5}; do echo; done

echo "############ building with 232 ################"
./gradlew clean buildPlugin -PbuildProfile=p232
cp build/distributions/digma-intellij-plugin*.zip .
for i in {1..5}; do echo; done

echo "############ building with 233 ################"
./gradlew clean buildPlugin -PbuildProfile=p233
cp build/distributions/digma-intellij-plugin*.zip .
for i in {1..5}; do echo; done

echo "############ building with 241 ################"
./gradlew clean buildPlugin -PbuildProfile=p241
cp build/distributions/digma-intellij-plugin*.zip .
for i in {1..5}; do echo; done

echo "############ building with 242 ################"
./gradlew clean buildPlugin -PbuildProfile=p242
cp build/distributions/digma-intellij-plugin*.zip .

echo "############ building with 243 ################"
./gradlew clean buildPlugin -PbuildProfile=p243
cp build/distributions/digma-intellij-plugin*.zip .
