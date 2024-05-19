# Code Runtime Analysis for Non-Trivial Java  Code

## Build

- ./gradlew clean buildPlugin [-PbuildProfile=XXX]</br>

## Load to Idea

- load the project into IntelliJ IDEA community with 'Open' and select the project directory</br>
- setup jdk 17 for the project 'Project Structure'</br>
- setup ProjectJdk or jdk 21 for gradle in 'Settings -> Gradle'</br>

## Load the dotnet project to Rider

- run ./gradlew :rider:prepare [-PbuildProfile=XXX]</br>
- open in rider the solution rider/Digma.Rider.Plugin/Digma.Rider.Plugin.sln</br>

### Build Profiles

build profiles build and package a plugin for specific intellij platform version.</br>
see the list of available profiles in  [BuildProfiles](common-build-logic/src/main/kotlin/common/BuildProfile.kt)</br>
for example './gradlew clean buildPlugin -PbuildProfile=p241'