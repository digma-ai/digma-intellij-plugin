package org.digma.intellij.plugin.idea.execution

const val JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS"
const val ORG_JAVA_TOOL_OPTIONS = "ORG_JAVA_TOOL_OPTIONS"
const val OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES"
const val ORG_OTEL_RESOURCE_ATTRIBUTES = "ORG_OTEL_RESOURCE_ATTRIBUTES"
const val DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE = "digma.environment"
const val DIGMA_OBSERVABILITY_ENV_VAR_NAME = "DIGMA_OBSERVABILITY"

const val OTEL_SERVICE_NAME_ENV_VAR_NAME = "OTEL_SERVICE_NAME"
const val OTEL_SERVICE_NAME_PROP_NAME = "otel.service.name"

const val DIGMA_MARKER = "-Dorg.digma.marker=true"


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
     * Maven spring boot
     */
    "stop",

    /*
     * Maven others
     */
    "build-image",

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
    "processResources",
    "processTestResources",
    "prepareKotlinBuildScriptModel",
    "resolveMainClassName"

    )