package org.digma.intellij.plugin.rider.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService;
import org.jetbrains.annotations.NotNull;

public class RiderPsiUtils {

    private RiderPsiUtils() {
    }

    @NotNull
    public static String psiFileToDocumentProtocolKey(@NotNull PsiFile psiFile) {
        return psiFile.getVirtualFile().toNioPath().toString();
    }

    public static boolean isCsharpLanguage(Language language, Project project) {
        //if not in rider this code will throw a CNF exception for LifetimedProjectComponent
        try {
            if (project.getService(CSharpLanguageService.class).isServiceFor(language)) {
                return true;
            }
        } catch (Exception e) {
            //ignore
        }

        return false;
    }
}
