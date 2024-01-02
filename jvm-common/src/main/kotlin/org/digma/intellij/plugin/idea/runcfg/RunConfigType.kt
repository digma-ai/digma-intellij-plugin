package org.digma.intellij.plugin.idea.runcfg

enum class RunConfigType(val isTest: Boolean) {
    JavaRun(false),
    KotlinRun(false),
    GradleRun(false),
    MavenRun(false),
    JavaTest(true),
    MavenTest(true),
    GradleTest(true),
    TomcatForIdeaUltimate(false),
    EeAppSeverAtIdeaUltimate(false),
    Unknown(false),
    ;
}
