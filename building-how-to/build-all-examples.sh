#!/bin/bash

#####  examples how to run various builds and runIde

set -e
###################   idea community

# there is no need to send the oldest profile, its the default if not sent,its here just as example

./gradlew clean buildPlugin -PbuildProfile=p241
./gradlew clean buildPlugin -PbuildProfile=p242
./gradlew clean buildPlugin -PbuildProfile=p243
./gradlew clean buildPlugin -PbuildProfile=p251

## to run ide from command line
./gradlew clean buildPlugin runIde
./gradlew clean buildPlugin runIde -PbuildProfile=p241
./gradlew clean buildPlugin runIde -PbuildProfile=p242
./gradlew clean buildPlugin runIde -PbuildProfile=p243
./gradlew clean buildPlugin runIde -PbuildProfile=p251


## run idea community with python plugin installed , this is for testing that digma functions correctly
## when python plugin is installed.
## the python plugin version need to be updated when updating build profiles
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:231.8770.65 -PbuildProfile=p241
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.8660.48 -PbuildProfile=p242
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.8660.48 -PbuildProfile=p243
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.8660.48 -PbuildProfile=p251


###################### idea ultimate

./gradlew clean buildPlugin -PbuildWIthUltimate=true
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p241
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p242
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p243
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p251

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p241
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p242
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p243
./gradlew clean buildPlugin runIde -PbuildWIthUltimate=true -PbuildProfile=p251




#################  rider
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p241
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p241
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p242
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p251

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=p241
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=p242
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=p243
./gradlew clean buildPlugin runIde -PbuildWithRider=true -PbuildProfile=p251


######################  pycharm

#./gradlew clean buildPlugin -PbuildWithPycharm=true
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p231
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p232

## to run ide from command line
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=p241
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=p242
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=p243
./gradlew clean buildPlugin runIde -PbuildWithPycharm=true -PbuildProfile=p251

######################  pycharm pro

#./gradlew clean buildPlugin -PbuildWithPycharmPro=true
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p231
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p232

## to run ide from command line
#./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true
#./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true -PbuildProfile=p231
#./gradlew clean buildPlugin runIde -PbuildWithPycharmPro=true -PbuildProfile=p232
