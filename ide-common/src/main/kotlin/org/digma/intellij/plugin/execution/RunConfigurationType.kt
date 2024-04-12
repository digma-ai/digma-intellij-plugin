package org.digma.intellij.plugin.execution


/**
 * this type is only used for logging and reporting. we don't rely on this type for any decision.
 */
enum class RunConfigurationType {
    Java,
    JavaTest,
    Kotlin,
    Gradle,
    Maven,
    Jar,
    JavaSever,
    Unknown;
}
