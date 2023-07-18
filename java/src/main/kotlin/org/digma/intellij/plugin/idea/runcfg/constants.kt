package org.digma.intellij.plugin.idea.runcfg

const val JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS"

const val OTEL_AGENT_JAR_URL = "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
const val DIGMA_AGENT_EXTENSION_JAR_URL = "https://github.com/digma-ai/otel-java-instrumentation/releases/latest/download/digma-otel-agent-extension.jar"
const val OTEL_AGENT_JAR_NAME = "opentelemetry-javaagent.jar"
const val DIGMA_AGENT_EXTENSION_JAR_NAME = "digma-otel-agent-extension.jar"

val KNOWN_IRRELEVANT_TASKS = setOf(
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
     * Maven - Quarkus
     */
    "quarkus:add-extension",
    "quarkus:add-extensions",
    "quarkus:analyze-call-tree",
    "quarkus:build",
    "quarkus:create",
    "quarkus:create-extension",
    "quarkus:create-jbang",
    "quarkus:dependency-tree",
    "quarkus:deploy",
    "quarkus:generate-code",
    "quarkus:generate-code-tests",
    "quarkus:go-offline",
    "quarkus:help",
    "quarkus:image-build",
    "quarkus:image-push",
    "quarkus:info",
    "quarkus:list-categories",
    "quarkus:list-extensions",
    "quarkus:list-platforms",
    "quarkus:prepare",
    "quarkus:prepare-tests",
    "quarkus:remove-extension",
    "quarkus:remove-extensions",
    "quarkus:test",
    "quarkus:update",

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

    /*
     * Gradle -> other
     */
    "compileJava",
    "compileKotlin",
    "compileTestJava",
    "compileTestKotlin",

)