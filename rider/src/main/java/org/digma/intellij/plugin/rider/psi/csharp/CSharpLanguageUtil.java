package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.lang.Language;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class CSharpLanguageUtil {


    //jetbrains changed package names of CSharpLanguage and CSharpFile in 2023.3.
    //so using reflection to support older versions.
    //todo: when our lowest version becomes 2023.3 we can remove all these methods and use these two classes regularly


    public static Language getCSharpLanguageInstanceWithReflection() {

        try {
            Class<? extends Language> csharpLanguageClass = getCSharpLanguageClassWithReflection();
            return (Language) csharpLanguageClass.getDeclaredField("INSTANCE").get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

    }


    @SuppressWarnings("unchecked")
    public static Class<? extends Language> getCSharpLanguageClassWithReflection() {

        try {
            return (Class<? extends Language>) Class.forName("com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage");
        } catch (ReflectiveOperationException e) {
            try {
                return (Class<? extends Language>) Class.forName("com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public static boolean isCSharpLanguage(@NotNull Language language) {
        return getCSharpLanguageClassWithReflection().isInstance(language);
    }

    public static boolean isCSharpFile(@NotNull PsiFile psiFile) {
//        return getCSharpFileClassWithReflection().isInstance(psiFile);
        return isCSharpLanguage(psiFile.getLanguage());
    }


    @SuppressWarnings("unchecked")
    public static Class<? extends PsiFile> getCSharpFileClassWithReflection() {

        try {
            return (Class<? extends PsiFile>) Class.forName("com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpFile");
        } catch (ReflectiveOperationException e) {
            try {
                return (Class<? extends PsiFile>) Class.forName("com.jetbrains.rider.languages.fileTypes.csharp.psi.CSharpFile");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


}
