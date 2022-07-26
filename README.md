# digma-jetbrains-plugin

<!-- https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax -->
<!-- Plugin description -->
**Digma is a Continuous Feedback platform. It integrates with your observability
backend to provide meaningful code insights in the IDE. While coding,
you can be aware of how the code behaves in runtime in various staging and production environments**

### Digma provides integrated insights multiple code behaviour topics including:

- Error hotspots
- Usage bottlenecks and concurrency
- Performance trends

<!-- Plugin description end -->

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


### Rider

Rider implementation is in rider module. </br>
The solution can be opened with rider or vscode - rider/Digma.Rider.Plugin</br>



### Publishing

run plugin verifier. verify plugin takes the IDEs to verify against from task listProductsReleases task</br>

./gradlew runPluginVerifier -Dorg.gradle.jvmargs=-Xmx2024m</br>

sign the plugin.</br>

Provide Your Personal Access Token to Gradle</br>
export DIGMA_JB_INTELLIJ_PUBLISH_TOKEN='YOUR_TOKEN'</br>


### Signing

https://plugins.jetbrains.com/docs/intellij/plugin-signing.html</br>
to sign the plugin set the following environment variables</br>

export:</br>
DIGMA_JB_CERTIFICATE_CHAIN_FILE=/home/shalom/workspace/digma/digma-intellij-plugin/.keys/chain.crt</br>
DIGMA_JB_PRIVATE_KEY_FILE=/home/shalom/workspace/digma/digma-intellij-plugin/.keys/private.pem</br>
DIGMA_JB_PRIVATE_KEY_PASSWORD=digma</br>


./gradlew signPlugin</br>


