package org.digma.intellij.plugin.idea.runcfg

enum class RunConfigType(val isTest: Boolean) {
    JavaRun(false),
    GradleRun(false),
    MavenRun(false),
    JavaTest(true),
    MavenTest(true),
    GradleTest(true),
    Unknown(false),
    ;
}
