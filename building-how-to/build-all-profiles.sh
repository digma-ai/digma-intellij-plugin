#!/bin/bash

#####  This script will build all profiles, will take a long time
## from the project root execute ./building-how-to/build-all-profiles.sh

set -e
###################   idea community

# there is no need to send the oldest profile, its the default if not sent. its here just as example

## check that profile aliases work
echo "############ building with lowest ################"
./gradlew clean buildPlugin -PbuildProfile=lowest
for i in {1..5}; do echo; done

echo "############ building with latest ################"
./gradlew clean buildPlugin -PbuildProfile=latest
for i in {1..5}; do echo; done

echo "############ building with eap ################"
./gradlew clean buildPlugin -PbuildProfile=eap
for i in {1..5}; do echo; done

echo "############ building with default ################"
./gradlew clean buildPlugin
for i in {1..5}; do echo; done

echo "############ building with 241 ################"
./gradlew clean buildPlugin -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with 242 ################"
./gradlew clean buildPlugin -PbuildProfile=p242
for i in {1..5}; do echo; done

echo "############ building with 243 ################"
./gradlew clean buildPlugin -PbuildProfile=p243
for i in {1..5}; do echo; done

echo "############ building with 241 ################"
./gradlew clean buildPlugin -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with 242 ################"
./gradlew clean buildPlugin -PbuildProfile=p242
for i in {1..5}; do echo; done

###################### idea ultimate

echo "############ building with ultimate default ################"
./gradlew clean buildPlugin -PbuildWIthUltimate=true
for i in {1..5}; do echo; done

echo "############ building with ultimate 241 ################"
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with ultimate 242 ################"
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p242
for i in {1..5}; do echo; done

echo "############ building with ultimate 243 ################"
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p243
for i in {1..5}; do echo; done

echo "############ building with ultimate 241 ################"
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with ultimate 242 ################"
./gradlew clean buildPlugin -PbuildWIthUltimate=true -PbuildProfile=p242
for i in {1..5}; do echo; done




#################  rider
echo "############ building with rider default ################"
./gradlew clean buildPlugin -PbuildWithRider=true
for i in {1..5}; do echo; done

echo "############ building with rider 241 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with rider 242 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p242
for i in {1..5}; do echo; done

echo "############ building with rider 243 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p243
for i in {1..5}; do echo; done

echo "############ building with rider 241 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p241
for i in {1..5}; do echo; done

echo "############ building with rider 242 ################"
./gradlew clean buildPlugin -PbuildWithRider=true -PbuildProfile=p242
for i in {1..5}; do echo; done