package org.digma.intellij.plugin.rider.psi.csharp

import com.intellij.lang.Language
import com.intellij.psi.PsiFile

object CSharpLanguageUtil {


    val cSharpLanguageInstanceWithReflection: Language?
        get() {
            try {
                val csharpLanguageClass: Class<out Language?> = cSharpLanguageClassWithReflection
                return csharpLanguageClass.getDeclaredField("INSTANCE").get(null) as Language?
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
        }


    @Suppress("UNCHECKED_CAST")
    val cSharpLanguageClassWithReflection: Class<out Language> by lazy {
        //jetbrains changed the package names of CSharpLanguage and CSharpFile in 2023.3.
        try {
            Class.forName("com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage") as Class<out Language?>
        } catch (_: ReflectiveOperationException) {
            try {
                Class.forName("com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage") as Class<out Language?>
            } catch (ex: ClassNotFoundException) {
                throw RuntimeException(ex)
            }
        }
    }


    fun isCSharpLanguage(language: Language): Boolean {
        return cSharpLanguageClassWithReflection.isInstance(language)
    }

    fun isCSharpFile(psiFile: PsiFile): Boolean {
//        return cSharpFileClassWithReflection.isInstance(psiFile);
        return isCSharpLanguage(psiFile.language)
    }


    @Suppress("UNCHECKED_CAST")
    val cSharpFileClassWithReflection: Class<out PsiFile?> by lazy {
        //jetbrains changed the package names of CSharpLanguage and CSharpFile in 2023.3.
        try {
            Class.forName("com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpFile") as Class<out PsiFile?>
        } catch (_: ReflectiveOperationException) {
            try {
                Class.forName("com.jetbrains.rider.languages.fileTypes.csharp.psi.CSharpFile") as Class<out PsiFile?>
            } catch (ex: ClassNotFoundException) {
                throw RuntimeException(ex)
            }
        }
    }
}