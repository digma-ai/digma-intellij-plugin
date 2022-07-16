package org.digma.intellij.plugin.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific services.
 */
public class PsiUtils {

    private static final Logger LOGGER = Logger.getInstance(PsiUtils.class);

    @Nullable
    public static MethodUnderCaret detectMethodUnderCaret(Project project, LanguageService languageService, int caretOffset, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
    }


    /*
    This method should either succeed or throw exception,never return null
     */
    @NotNull
    public static PsiFile uriToPsiFile(@NotNull String uri, @NotNull Project project) throws PsiFileNotFountException {
        return ReadAction.compute(() -> {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(uri);
            if (virtualFile == null){
                throw new PsiFileNotFountException("could not locate VirtualFile for uri "+uri);
            }
            var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile == null){
                throw new PsiFileNotFountException("could not locate PsiFile for uri "+uri+", virtual file:"+virtualFile);
            }
            return psiFile;
        });
    }




    @NotNull
    public static String psiFileToDocumentProtocolKey(@NotNull PsiFile psiFile){
        return psiFile.getVirtualFile().toNioPath().toString();
    }
}
