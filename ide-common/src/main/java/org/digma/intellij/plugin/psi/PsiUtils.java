package org.digma.intellij.plugin.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific services.
 */
public class PsiUtils {

    private PsiUtils() {
    }

//    @Nullable
//    public static MethodUnderCaret detectMethodUnderCaret(Project project, LanguageService languageService, int caretOffset, VirtualFile file) {
//        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
//        return languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
//    }


    /*
    Note: our DocumentInfo object has a fileUri which is a uri including the 'file:' schema and is used to convert PsiFile
    to and from string. it is mainly used to store the file uri that the document belongs to and when necessary file the
    PsiFile.
     */

    @NotNull
    public static String psiFileToUri(@NotNull PsiFile psiFile) {
        return psiFile.getVirtualFile().getUrl();
    }

    /*
    This method should either succeed or throw exception,never return null
     */
    @NotNull
    public static PsiFile uriToPsiFile(@NotNull String uri, @NotNull Project project) throws PsiFileNotFountException {
        //todo: check if ReadAction is necessary
        return ReadAction.compute(() -> {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(uri);
            if (virtualFile == null) {
                throw new PsiFileNotFountException("could not locate VirtualFile for uri "+uri);
            }
            var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile == null){
                throw new PsiFileNotFountException("could not locate PsiFile for uri "+uri+", virtual file:"+virtualFile);
            }
            return psiFile;
        });
    }

}
