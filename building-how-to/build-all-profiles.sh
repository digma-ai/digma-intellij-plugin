#!/bin/bash

#####  This script will build all profiles, will take a long time
## from the project root execute ./building-how-to/build-all-profiles.sh

set -e
###################   idea community

# there is no need to send the lowest profile, its the default if not sent,its here just as example

./gradlew clean buildPlugin runPluginVerifier -PbuildProfile=lowest
./gradlew clean buildPlugin runPluginVerifier -PbuildProfile=latest
./gradlew clean buildPlugin runPluginVerifier -PbuildProfile=eap


###################### idea ultimate

./gradlew clean buildPlugin runPluginVerifier -PbuildWIthUltimate=true
./gradlew clean buildPlugin runPluginVerifier -PbuildWIthUltimate=true -PbuildProfile=latest
./gradlew clean buildPlugin runPluginVerifier -PbuildWIthUltimate=true -PbuildProfile=eap



#################  rider
./gradlew clean buildPlugin -PbuildWithRider=true
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=eap

######################  pycharm

./gradlew clean buildPlugin -PbuildWithPycharm=true
./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=eap


######################  pycharm pro

./gradlew clean buildPlugin -PbuildWithPycharmPro=true
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=latest
./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=eap

