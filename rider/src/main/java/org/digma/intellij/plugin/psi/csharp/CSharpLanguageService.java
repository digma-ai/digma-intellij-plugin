package org.digma.intellij.plugin.psi.csharp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage;
import org.digma.intellij.plugin.model.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.Nullable;

public class CSharpLanguageService implements LanguageService {


    //should not be called !!
    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        return CSharpLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return CSharpLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public @Nullable MethodUnderCaret detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        throw new UnsupportedOperationException("should not be called");
    }

}
