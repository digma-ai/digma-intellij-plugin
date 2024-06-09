#!/bin/bash

set -e


echo "############ building with 231 ################"
./gradlew clean buildPlugin -PbuildProfile=p231
for i in {1..5}; do echo; done

echo "############ building with 232 ################"
./gradlew clean buildPlugin -PbuildProfile=p232
for i in {1..5}; do echo; done

echo "############ building with 233 ################"
./gradlew clean buildPlugin -PbuildProfile=p233
for i in {1..5}; do echo; done

echo "############ building with 241 ################"
./gradlew clean buildPlugin -PbuildProfile=p241
