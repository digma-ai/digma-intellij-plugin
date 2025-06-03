package org.digma.intellij.plugin.psi

enum class SupportedLanguages(val languageServiceClassName: String) {
    JAVA("org.digma.intellij.plugin.idea.java.JavaLanguageService"),
    KOTLIN("org.digma.intellij.plugin.idea.kotlin.KotlinLanguageService"),
    CSHARP("org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService"),
    PYTHON("org.digma.intellij.plugin.python.PythonLanguageService");
}
