#!/bin/bash

#####  examples how to run various builds and runIde

set -e
###################   idea community

# there is no need to send the oldest profile, its the default if not sent,its here just as example

./gradlew clean buildPlugin -PbuildProfile=p223
./gradlew clean buildPlugin -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildProfile=p232
./gradlew clean buildPlugin -PbuildProfile=p233
./gradlew clean buildPlugin -PbuildProfile=p241

## to run ide from command line
./gradlew clean buildPlugin runIde
./gradlew clean buildPlugin runIde -PbuildProfile=p231
./gradlew clean buildPlugin runIde -PbuildProfile=p232
./gradlew clean buildPlugin runIde -PbuildProfile=p233
./gradlew clean buildPlugin runIde -PbuildProfile=p241


## run idea community with python plugin installed , this is for testing that digma functions correctly
## when python plugin is installed.
## the python plugin version need to be updated when updating build profiles
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:223.7571.182 -PbuildProfile=p223
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:231.8770.65 -PbuildProfile=p231
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.8660.48 -PbuildProfile=p232
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.8660.48 -PbuildProfile=p233
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.8660.48 -PbuildProfile=p241


###################### idea ultimate

./gradlew clean buildPlugin -PbuildWIthUltimate=true
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p232
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p233
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p241

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p231
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p232
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p233
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p241




#################  rider
./gradlew clean buildPlugin -PbuildWithRider=true
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p232

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithRider=true
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=p231
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=p232


######################  pycharm

./gradlew clean buildPlugin -PbuildWithPycharm=true
./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p232

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=p231
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=p232

######################  pycharm pro

./gradlew clean buildPlugin -PbuildWithPycharmPro=true
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p232

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true
./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true -PbuildProfile=p231
./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true -PbuildProfile=p232
