#!/bin/bash

set -e


echo "############ verifyPlugin with 241 ################"
./gradlew clean verifyPlugin -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ verifyPlugin with 242 ################"
./gradlew clean verifyPlugin -PbuildProfile=p242
for i in {1..5}; do echo; done

echo "############ verifyPlugin with 243 ################"
./gradlew clean verifyPlugin -PbuildProfile=p243
for i in {1..5}; do echo; done

echo "############ verifyPlugin with 251 ################"
./gradlew clean verifyPlugin -PbuildProfile=p251
