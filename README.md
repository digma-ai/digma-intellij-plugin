# digma-jetbrains-plugin

TBD


## Build
The project must be built with jdk 11 or later as the gradle jdk.</br>

./gradlew buildPlugin -Porg.gradle.java.home=<PATH TO JDK 11></br>
or</br>
gradlew.bat buildPlugin -Porg.gradle.java.home=<PATH TO JDK 11></br>

## Load to Idea

- load the project into IntelliJ IDEA community with 'Open' and select the project directory</br>
- setup jdk 11 for the project 'Project Structure'</br>
- setup ProjectJdk for gradle in 'Settings -> Gradle'</br>


### Local ide testing with development instances

- in gradle.properties change 'platformType' to any of the supported IDEs, default is Rider</br>
- open gradle tool window in idea and execute task intellij.runIde. or from the 'Run/Debug configuration' load and run 'Run Plugin'</br>
- load a relevant project in the development instance</br>
- play with the plugin</br>

(when testing with IC or IU or RD it is possible to install the python plugin on the development instance 
and test python projects too)


<!-- Plugin description -->
[//]: # (satisfy gradle build with plugin description)
<!-- Plugin description end -->

  
[template]: https://github.com/JetBrains/intellij-platform-plugin-template
