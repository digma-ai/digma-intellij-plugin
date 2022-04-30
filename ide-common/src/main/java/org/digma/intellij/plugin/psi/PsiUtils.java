package org.digma.intellij.plugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific services.
 */
public class PsiUtils {

    private static final Logger LOGGER = Logger.getInstance(PsiUtils.class);

    @Nullable
    public static MethodIdentifier detectMethodUnderCaret(Project project, LanguageService languageService, int caretOffset, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
    }

}
