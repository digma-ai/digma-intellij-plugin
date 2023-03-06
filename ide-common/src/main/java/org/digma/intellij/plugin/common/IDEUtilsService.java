package org.digma.intellij.plugin.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

@Deprecated //not necessary, keep as example
public class IDEUtilsService {

    private final IsRider isRider;


    public IDEUtilsService(Project project) {
        isRider = new IsRider(project);
    }

    public boolean isRiderAndCSharpFile(@NotNull Project project, VirtualFile file) {

        //it may be a C# file that was opened from vcs, it doesn't count as C# that CSharpLanguageService should handle
        if (file == null) {
            return false;
        }

        if (isRider.isRider()) {
            LanguageService csharpLanguageService = isRider.getCSharpLanguageService();
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                return false;
            }
            return csharpLanguageService.isServiceFor(psiFile.getLanguage());
        }

        return false;
    }

    public boolean isRider() {
        return isRider.isRider();
    }





    private static class IsRider {

        private LanguageService myLanguageService = null;

        public IsRider(Project project) {
            init(project);
        }

        @SuppressWarnings("unchecked")
        private void init(Project project) {
            Class<LanguageService> cshrpLanguageServiceClass;
            try {
                cshrpLanguageServiceClass = (Class<LanguageService>) Class.forName("org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService");
                myLanguageService = project.getService(cshrpLanguageServiceClass);
            } catch (Throwable ignored) {
            }
        }

        public boolean isRider() {
            return myLanguageService != null;
        }

        public LanguageService getCSharpLanguageService() {
            return myLanguageService;
        }
    }
}
