package org.digma.intellij.plugin.psi;

public enum SupportedLanguages {

    JAVA("org.digma.intellij.plugin.idea.psi.java.JavaLanguageService"),
    KOTLIN("org.digma.intellij.plugin.idea.psi.kotlin.KotlinLanguageService"),
    CSHARP("org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService"),
    PYTHON("org.digma.intellij.plugin.psi.python.PythonLanguageService");

    private final String languageServiceClassName;

    SupportedLanguages(String languageServiceClassName) {
        this.languageServiceClassName = languageServiceClassName;
    }

    public String getLanguageServiceClassName() {
        return languageServiceClassName;
    }
}
