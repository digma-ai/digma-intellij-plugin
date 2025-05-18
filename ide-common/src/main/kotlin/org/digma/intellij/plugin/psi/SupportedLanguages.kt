package org.digma.intellij.plugin.psi

enum class SupportedLanguages(val languageServiceClassName: String) {
    JAVA("org.digma.intellij.plugin.idea.psi.java.JavaLanguageService"),
    KOTLIN("org.digma.intellij.plugin.idea.psi.kotlin.KotlinLanguageService"),
    CSHARP("org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService"),
    PYTHON("org.digma.intellij.plugin.psi.python.PythonLanguageService");
}
