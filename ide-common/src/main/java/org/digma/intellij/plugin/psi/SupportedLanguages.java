package org.digma.intellij.plugin.psi;

public enum SupportedLanguages {

    CSHARP("org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService"),
    PYTHON("org.digma.intellij.plugin.psi.python.PythonLanguageService"),
    JAVA("org.digma.intellij.plugin.idea.psi.java.JavaLanguageService"),
    KOTLIN("org.digma.intellij.plugin.idea.psi.kotlin.KotlinLanguageService");

    private final String languageServiceClassName;

    SupportedLanguages(String languageServiceClassName) {
        this.languageServiceClassName = languageServiceClassName;
    }

    public String getLanguageServiceClassName() {
        return languageServiceClassName;
    }
}
