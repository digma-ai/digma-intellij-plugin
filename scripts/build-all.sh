#!/bin/bash

set -e

./gradlew clean buildPlugin

./gradlew clean buildPlugin -PuseLatestVersion=true

./gradlew clean buildPlugin -PuseEAPVersion=true

