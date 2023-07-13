#!/bin/bash

#####  examples how to run various builds and runIde

set -e
###################   idea community

# there is no need to send the lowest profile, its the default if not sent,its here just as example

./gradlew clean buildPlugin -PbuildProfile=lowest
./gradlew clean buildPlugin -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildProfile=eap

## to run ide from command line
./gradlew clean buildPlugin runIde
./gradlew clean buildPlugin runIde -PbuildProfile=latest
./gradlew clean buildPlugin runIde -PbuildProfile=eap


## run idea community with python plugin installed , this is for testing that digma functions correctly
## when python plugin is installed.
## the python plugin version need to be updated when updating build profiles
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:223.7571.182 -PbuildProfile=lowest
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:231.8770.65 -PbuildProfile=latest
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.8660.48 -PbuildProfile=eap


###################### idea ultimate

./gradlew clean buildPlugin -PbuildWIthUltimate=true
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=eap

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=latest
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=eap




#################  rider
./gradlew clean buildPlugin -PbuildWithRider=true
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=eap

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithRider=true
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=latest
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=eap


######################  pycharm

./gradlew clean buildPlugin -PbuildWithPycharm=true
./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=eap

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=latest
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=eap

######################  pycharm pro

./gradlew clean buildPlugin -PbuildWithPycharmPro=true
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=eap

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true
./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true -PbuildProfile=latest
./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true -PbuildProfile=eap
