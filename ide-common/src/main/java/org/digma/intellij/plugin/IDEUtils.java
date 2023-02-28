package org.digma.intellij.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

public class IDEUtils {

    public static boolean isRiderAndCSharpFile(@NotNull Project project, VirtualFile file) {

        if (file == null){
            return false;
        }

        Class<LanguageService> cshrpLanguageServiceClass;
        try {
            cshrpLanguageServiceClass = (Class<LanguageService>) Class.forName("org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService");
        } catch (Throwable e) {
            return false;
        }


        try {
            LanguageService csharpLanguageService = project.getService(cshrpLanguageServiceClass);
            if (csharpLanguageService == null){
                return false;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null){
                return false;
            }
            return csharpLanguageService.isServiceFor(psiFile.getLanguage());
        } catch (Exception e) {
            return false;
        }
    }

   public static boolean isRider(@NotNull Project project) {
        try {
            var service = project.getService(Class.forName("org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService"));
            return service != null;
        } catch (Exception e) {
            return false;
        }
    }

}
