package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface LanguageService {

    Logger LOGGER = Logger.getInstance(LanguageService.class);


    boolean isSupportedFile(Project project, VirtualFile newFile);

    boolean isSupportedFile(Project project, PsiFile psiFile);

    MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, int caretOffset);



    //for use by language services that have a psi impl, python, java, Not rider
    @Nullable
    default PsiElement findElementUnderCaret(@NotNull Project project, PsiFile psiFile, int caretOffset) {
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

    /**
     * this method has kind of the same purpose as isIntellijPlatformPluginLanguage , to distinguish languages that are
     * indexed in intellij FileBasedIndex
     *
     * @return true if this language in indexed in FileBasedIndex
     */
    boolean isIndexedLanguage();


    DocumentInfo buildDocumentInfo(PsiFile psiFile);

    /**
     * This method is meant to distinguish languages that are implemented as intellij platform plugin.
     * the main reason is that we need to know if this is not C# on rider. its used mainly to help in decisions about
     * how to process swing editor events.
     * for example, C# is not relevant for processing editor caret events or file open events because its implemented
     * in resharper and the plugin is notified about these events from resharper.
     *
     * @return true if this is an intellij platform event.
     */
    boolean isIntellijPlatformPluginLanguage();
}
