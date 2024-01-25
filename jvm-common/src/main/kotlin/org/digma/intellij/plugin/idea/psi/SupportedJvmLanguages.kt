package org.digma.intellij.plugin.idea.psi

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import org.digma.intellij.plugin.psi.SupportedLanguages
import org.jetbrains.kotlin.idea.KotlinLanguage

enum class SupportedJvmLanguages(val language: SupportedLanguages, val languageInstance: Language) {

    JAVA(SupportedLanguages.JAVA, JavaLanguage.INSTANCE),
    KOTLIN(SupportedLanguages.KOTLIN, KotlinLanguage.INSTANCE)

}