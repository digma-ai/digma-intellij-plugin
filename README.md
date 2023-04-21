# Code Runtime Analysis for Non-Trivial Java  Code

<!-- https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax -->
<!-- Plugin description -->

Digma is an IDE plugin for analyzing code runtime data.
It enables rapid development in complex
projects by linting and detecting issues as they appear, highlighting possible risks in code and providing code change analysis and context

### Digma lints common code smells and issues as you code

- Error hotspots
- Bottlenecks and concurrency
- Query anti-patterns
  - Identify risks and affected code for code changes
- Performance trends

For more info check out the Digma [main repo](https://github.com/digma-ai/digma)

<!-- Plugin description end -->


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


