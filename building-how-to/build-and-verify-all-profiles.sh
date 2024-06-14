#!/bin/bash

#####  This script will build all profiles, will take a long time
## from the project root execute ./building-how-to/build-all-profiles.sh

set -e
###################   idea community

# there is no need to send the oldest profile, its the default if not sent. its here just as example

## check that profile aliases work
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=lowest
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=latest
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=eap

./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p231
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p232
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p233
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p241
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p242


###################### idea ultimate

./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p231
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p232
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p233
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p241
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p242



#################  rider
./gradlew clean buildPlugin -PbuildWithRider=true
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p231
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p232
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p233
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p241
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p242

######################  pycharm

#./gradlew clean buildPlugin -PbuildWithPycharm=true
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p231
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p232
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p233
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p241
#./gradlew clean buildPlugin -PbuildWithPycharm=true -PbuildProfile=p242


######################  pycharm pro

#./gradlew clean buildPlugin -PbuildWithPycharmPro=true
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p231
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p232
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p233
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p241
#./gradlew clean buildPlugin -PbuildWithPycharmPro=true -PbuildProfile=p242

