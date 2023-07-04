#!/bin/bash

#####  examples how to run various builds and runIde

set -e
###################   idea community

./gradlew clean buildPlugin
./gradlew clean buildPlugin -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildProfile=eap

## to run ide from command line
./gradlew clean runIde
./gradlew clean runIde -PbuildProfile=latest
./gradlew clean runIde -PbuildProfile=eap


aaa
## to run ide from command line
./gradlew clean runIde -PplatformPlugins=com.intellij.java
./gradlew clean runIde -PplatformPlugins=com.intellij.java -PuseLatestVersion=true
./gradlew clean runIde -PplatformPlugins=com.intellij.java -PuseEAPVersion=true

## run idea community with python plugin installed , this is for testing that digma functions correctly
## when python plugin is installed
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:231.8770.65
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:231.8770.65 -PuseLatestVersion=true
./gradlew clean runIde -PplatformPlugins=com.intellij.java,PythonCore:232.7295.16 -PuseEAPVersion=true


###################### idea ultimate

./gradlew clean buildPlugin -PbuildWIthUltimate=true
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PuseLatestVersion=true
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PuseEAPVersion=true

## to run ide from command line
./gradlew clean runIde -PbuildWIthUltimate=true -PplatformPlugins=com.intellij.java
./gradlew clean runIde -PbuildWIthUltimate=true -PplatformPlugins=com.intellij.java -PuseLatestVersion=true
./gradlew clean runIde -PbuildWIthUltimate=true -PplatformPlugins=com.intellij.java -PuseEAPVersion=true




#################  rider
./gradlew clean buildPlugin -PbuildWithRider=true -PplatformPlugins=rider-plugins-appender
./gradlew clean buildPlugin -PbuildWithRider=true -PplatformPlugins=rider-plugins-appender -PuseLatestVersion=true
./gradlew clean buildPlugin -PbuildWithRider=true -PplatformPlugins=rider-plugins-appender -PuseEAPVersion=true

## to run ide from command line
./gradlew clean runIde -PbuildWithRider=true -PplatformPlugins=rider-plugins-appender
./gradlew clean runIde -PbuildWithRider=true -PplatformPlugins=rider-plugins-appender -PuseLatestVersion=true
./gradlew clean runIde -PbuildWithRider=true -PplatformPlugins=rider-plugins-appender -PuseEAPVersion=true


######################  pycharm

./gradlew clean buildPlugin -PbuildWithPycharm=true -PplatformPlugins=
./gradlew clean buildPlugin -PbuildWithPycharm=true -PplatformPlugins= -PuseLatestVersion=true
./gradlew clean buildPlugin -PbuildWithPycharm=true -PplatformPlugins= -PuseEAPVersion=true

## to run ide from command line
./gradlew clean runIde -PbuildWithPycharm=true -PplatformPlugins=
./gradlew clean runIde -PbuildWithPycharm=true -PplatformPlugins= -PuseLatestVersion=true
./gradlew clean runIde -PbuildWithPycharm=true -PplatformPlugins= -PuseEAPVersion=true

######################  pycharm pro

./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PplatformPlugins=
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PplatformPlugins= -PuseLatestVersion=true
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PplatformPlugins= -PuseEAPVersion=true

## to run ide from command line
./gradlew clean runIde -PbuildWithPycharmPro=true -PplatformPlugins=
./gradlew clean runIde -PbuildWithPycharmPro=true -PplatformPlugins= -PuseLatestVersion=true
./gradlew clean runIde -PbuildWithPycharmPro=true -PplatformPlugins= -PuseEAPVersion=true
