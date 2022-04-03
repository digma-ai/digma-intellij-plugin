# digma-jetbrains-plugin

TBD


## build
The project must be built with jdk 11 or later as the gradle jdk.

./gradlew buildPlugin -Porg.gradle.java.home=<PATH TO JDK 11>

## testing

- load the project into IntelliJ IDEA community 2021.3.3</br>
- setup jdk 11 for the project</br>


### idea

- open gradle tool window and execute task intellij.runIde</br>
- on the idea IC that loads install the python community plugin</br> 
- load example-fastapi-app</br>
- play with the plugin</br>
- load sample-projects/simple-idea-java-project into the same window</br>
- play with the plugin</br>

### pycharm

- install pycharm on your machine</br>
- build the plugin: ./gradlew buildPlugin -Porg.gradle.java.home=<PATH TO JDK 11></br>
- copy or link build/libs/digma-intellij-plugin-0.0.1.jar into PYCHARM_HOME/plugins and rename to Digma.jar</br>
- run pycharm and load example-fastapi-app
- play with the plugin</br>



<!-- Plugin description -->
[//]: # (satisfy gradle build with plugin description)
<!-- Plugin description end -->

  
[template]: https://github.com/JetBrains/intellij-platform-plugin-template
