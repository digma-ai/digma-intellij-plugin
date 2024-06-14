#!/bin/bash

set -e


echo "############ verifyPlugin with 231 ################"
./gradlew clean verifyPlugin -PbuildProfile=p231
for i in {1..5}; do echo; done

echo "############ verifyPlugin with 232 ################"
./gradlew clean verifyPlugin -PbuildProfile=p232
for i in {1..5}; do echo; done

echo "############ verifyPlugin with 233 ################"
./gradlew clean verifyPlugin -PbuildProfile=p233
for i in {1..5}; do echo; done

echo "############ verifyPlugin with 241 ################"
./gradlew clean verifyPlugin -PbuildProfile=p241

echo "############ verifyPlugin with 242 ################"
./gradlew clean verifyPlugin -PbuildProfile=p242
