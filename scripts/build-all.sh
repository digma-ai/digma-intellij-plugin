#!/bin/bash

set -e

./gradlew clean buildPlugin

./gradlew clean buildPlugin -PuseLatestVersion

./gradlew clean buildPlugin -PuseEAPVersion

