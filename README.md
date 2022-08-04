# Digma Jetbrain Plugin

<!-- https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax -->
<!-- Plugin description -->

**Digma is a Continuous Feedback platform. It integrates with your observability
backend to provide meaningful code insights in the IDE. While coding,
you can be aware of how the code behaves in runtime in various staging and production environments**

### Digma provides integrated insights multiple code behaviour topics including:

- Error hotspots
- Usage bottlenecks and concurrency
- Performance trends

For more info check out the Digma [main repo](https://github.com/digma-ai/digma)

<!-- Plugin description end -->

## Supported jetbrains IDEs

Rider</br>
more to come soon...

## Build

The project must be built with jdk 11 or later as the gradle jdk.</br>

./gradlew buildPlugin -Porg.gradle.java.home=<PATH TO JDK 11></br>
or</br>
gradlew.bat buildPlugin -Porg.gradle.java.home=<PATH TO JDK 11></br>

## Load to Idea

- load the project into IntelliJ IDEA community with 'Open' and select the project directory</br>
- setup jdk 11 for the project 'Project Structure'</br>
- setup ProjectJdk for gradle in 'Settings -> Gradle'</br>


### Local IDE testing with development instances

- in gradle.properties change 'platformType' to any of the supported IDEs, default is Rider</br>
- open gradle tool window in idea and execute task intellij.runIde. or from the 'Run/Debug configuration' load and run 'Run Plugin'</br>
- load a relevant project in the development instance</br>
- play with the plugin</br>

(when testing with IC or IU or RD it is possible to install the python plugin on the development instance 
and test python projects too)


### Rider

Rider implementation is in rider module. </br>
The dotnet solution can be opened with rider - rider/Digma.Rider.Plugin</br>
TBD: dotnet requirements


