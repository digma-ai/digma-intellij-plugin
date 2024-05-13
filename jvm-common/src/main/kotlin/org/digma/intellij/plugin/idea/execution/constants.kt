package org.digma.intellij.plugin.idea.execution

const val JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS"
const val OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES"
const val DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE = "digma.environment.id"
const val DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE = "digma.environment"
const val DIGMA_USER_ID_RESOURCE_ATTRIBUTE = "digma.user.id"
const val DIGMA_SCM_COMMIT_ID_RESOURCE_ATTRIBUTE = "scm.commit.id"

const val OTEL_SERVICE_NAME_ENV_VAR_NAME = "OTEL_SERVICE_NAME"
const val OTEL_SERVICE_NAME_PROP_NAME = "otel.service.name"

const val DIGMA_MARKER = "-Dorg.digma.marker=true"

/*
DIGMA_OBSERVABILITY is an environment variable to forces observability when user executes an unsupported
 gradle task or maven goal. it should be used only for gradle or maven. it can not force observability for unknown
 configuration types because we don't know how to treat unknown configuration types. gradle and maven configuration
 types are known to us, we just don't support all possible gradle task names or maven goals.
there can be two values for DIGMA_OBSERVABILITY, app or test. when app, the observability will be treated as regular application,
 when test, the observability will be treated as test and is mainly used to add digma.environment LOCAL or LOCAL_TESTS.
when exists and value is empty it will be treated as app.
to force a different instrumentation flavor add INSTRUMENTATION_FLAVOR environment variable with value one of
 org.digma.intellij.plugin.idea.execution.flavor.InstrumentationFlavorType.
examples:
if running a task called myTask that we don't support:
DIGMA_OBSERVABILITY or DIGMA_OBSERVABILITY=app -> will be treated as regula application with the default flavor
DIGMA_OBSERVABILITY=test -> will be treated as a test execution.

together with
DIGMA_OBSERVABILITY=test
INSTRUMENTATION_FLAVOR=Quarkus
will be treated as Quarkus flavor and a test execution.
*/
const val DIGMA_OBSERVABILITY = "DIGMA_OBSERVABILITY"

enum class DigmaObservabilityType { app, test }

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
    "resolveMainClassName",
    "runClientTester",
    "javaToolchains",
    "outgoingVariants",
    "resolvableConfigurations",
    "spotlessApply",
    "javaagentClasses",
    "testFixturesClasses",
    "app-server:server",
    "buildAndPushDockerImage",
    "runClientTester",
    "sonarqube",
    "qualityCheck",
    "integrationTestClasses",

    /*
     * Intellij plugin project
     */
    "buildPlugin",
    "runIde",
    "setupDependencies"

)