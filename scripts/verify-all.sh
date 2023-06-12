#!/bin/bash

## verify all IDEs from our lowest development version to latest EAP
set -e

## verify idea
./gradlew runPluginVerifier -PtypesToVerifyPlugin=IC,IU -PversionsToVerifyPlugin=2022.3.1,231.9161-EAP-CANDIDATE-SNAPSHOT

## verify pycharm
./gradlew runPluginVerifier -PtypesToVerifyPlugin=PC,PY -PversionsToVerifyPlugin=2022.3.1,232.7295-EAP-CANDIDATE-SNAPSHOT

## verify rider
./gradlew runPluginVerifier -PtypesToVerifyPlugin=RD -PversionsToVerifyPlugin=2022.3.1,2023.2-EAP4-SNAPSHOT


