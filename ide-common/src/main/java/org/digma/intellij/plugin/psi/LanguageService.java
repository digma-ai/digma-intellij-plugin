package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface LanguageService {

    Logger LOGGER = Logger.getInstance(LanguageService.class);


    boolean isSupportedFile(Project project, VirtualFile newFile);

    boolean isSupportedFile(Project project, PsiFile psiFile);

    @Nullable
    MethodUnderCaret detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset);



    //for use by language services that have a psi impl, python, java, Not rider
    @Nullable
    default PsiElement findElementUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        if (psiFile != null) {
            PsiElement psiElement = psiFile.findElementAt(caretOffset);
            Log.log(LOGGER::debug, "got psi element {}", psiElement);
            return psiElement;
        }
        return null;
    }


    void navigateToMethod(String codeObjectId);

    boolean isServiceFor(Language language);

    Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds);

    Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds);

    void environmentChanged(String newEnv);
}
