# An IntelliJ Plugin to Identify Code Performance Issues in Runtime

<!-- Plugin description -->

Digma is an IntelliJ plugin for automatically [identifying and fixing performance issues in your code](https://digma.ai/blog/introducing-the-digma-jetbrains-plugin/).
It enables developers to find the root cause of bottlenecks, scaling problems and query issues in the code. 

### Example of issues Digma detects automatically

- Bottlenecks and concurrency anti-patterns
- Query inefficiencies
- Scaling problems
- N+1 Selects 
- Performance regressions

For more info check out our [website](https://digma.ai)

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


