package org.digma.intellij.plugin.idea.runcfg

const val JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS"

const val OTEL_AGENT_JAR_URL = "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
const val DIGMA_AGENT_EXTENSION_JAR_URL = "https://github.com/digma-ai/otel-java-instrumentation/releases/latest/download/digma-otel-agent-extension.jar"
const val OTEL_AGENT_JAR_NAME = "opentelemetry-javaagent.jar"
const val DIGMA_AGENT_EXTENSION_JAR_NAME = "digma-otel-agent-extension.jar"

val UNIMPORTANT_TASKS = listOf(
    /*
     * Maven
     */
    "clean",
    "validate",
    "compile",
    "test",
    "package",
    "verify",
    "install",
    "site",
    "deploy",

    /*
     * Gradle -> build
     */
    "assemble",
    "build",
    "buildDependents",
    "buildNeeded",
    "classes",
    "clean",
    "jar",
    "testClasses",
    "buildKotlinToolingMetadata",
    "kotlinSourcesJar",
    "bootBuildImage",
    "bootJar",

    /*
     * Gradle -> build setup
     */
    "init",
    "wrapper",

    /*
     * Gradle -> documentation
     */
    "javadoc",

    /*
     * Gradle -> help
     */
    "buildEnvironment",
    "components",
    "dependencies",
    "dependencyInsight",
    "dependentComponents",
    "dependencyManagement",
    "help",
    "model",
    "projects",
    "properties",
    "tasks",

    /*
     * Gradle -> verification
     */
    "check",
    "test",

)