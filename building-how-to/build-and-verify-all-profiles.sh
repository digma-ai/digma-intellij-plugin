#!/bin/bash

#####  This script will build all profiles, will take a long time
## from the project root execute ./building-how-to/build-all-profiles.sh

set -e
###################   idea community

# there is no need to send the oldest profile, its the default if not sent. its here just as example

## check that profile aliases work
echo "############ building with lowest ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=lowest
for i in {1..5}; do echo; done

echo "############ building with latest ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=latest
for i in {1..5}; do echo; done

echo "############ building with eap ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=eap
for i in {1..5}; do echo; done

echo "############ building with default ################"
./gradlew clean buildPlugin verifyPlugin
for i in {1..5}; do echo; done

echo "############ building with 231 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p231
for i in {1..5}; do echo; done

echo "############ building with 232 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p232
for i in {1..5}; do echo; done

echo "############ building with 233 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p233
for i in {1..5}; do echo; done

echo "############ building with 241 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with 232 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildProfile=p242
for i in {1..5}; do echo; done

###################### idea ultimate

echo "############ building with ultimate default ################"
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true
for i in {1..5}; do echo; done

echo "############ building with ultimate 231 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p231
for i in {1..5}; do echo; done

echo "############ building with ultimate 232 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p232
for i in {1..5}; do echo; done

echo "############ building with ultimate 233 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p233
for i in {1..5}; do echo; done

echo "############ building with ultimate 241 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with ultimate 242 ################"
./gradlew clean buildPlugin verifyPlugin -PbuildWIthUltimate=true -PbuildProfile=p242
for i in {1..5}; do echo; done




#################  rider
echo "############ building with rider default ################"
./gradlew clean buildPlugin -PbuildWithRider=true
for i in {1..5}; do echo; done

echo "############ building with rider 231 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p231
for i in {1..5}; do echo; done

echo "############ building with rider 232 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p232
for i in {1..5}; do echo; done

echo "############ building with rider 233 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p233
for i in {1..5}; do echo; done

echo "############ building with rider 241 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with rider 242 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p242
for i in {1..5}; do echo; done

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

