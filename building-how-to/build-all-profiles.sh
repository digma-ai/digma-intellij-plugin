#!/bin/bash

#####  This script will build all profiles, will take a long time
## from the project root execute ./building-how-to/build-all-profiles.sh

set -e
###################   idea community

# there is no need to send the oldest profile, its the default if not sent. its here just as example

# todo: add 242

## check that profile aliases work
./gradlew clean buildPlugin -PbuildProfile=lowest
./gradlew clean buildPlugin -PbuildProfile=latest
#./gradlew clean buildPlugin -PbuildProfile=eap


./gradlew clean buildPlugin -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildProfile=p232
./gradlew clean buildPlugin -PbuildProfile=p233
./gradlew clean buildPlugin -PbuildProfile=p241


###################### idea ultimate

./gradlew clean buildPlugin -PbuildWIthUltimate=true
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p232
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p233
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p241



#################  rider
./gradlew clean buildPlugin -PbuildWithRider=true
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p232
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p233
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p241

######################  pycharm

#./gradlew clean buildPlugin -PbuildWithPycharm=true
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p231
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p232
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p233
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p241


######################  pycharm pro

#./gradlew clean buildPlugin -PbuildWithPycharmPro=true
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p231
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p232
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p233
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p241

