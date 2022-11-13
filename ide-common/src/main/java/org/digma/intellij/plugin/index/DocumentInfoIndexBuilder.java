package org.digma.intellij.plugin.index;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.jetbrains.annotations.NotNull;

public class DocumentInfoIndexBuilder {

    private final LanguageServiceLocator languageServiceLocator;

    public DocumentInfoIndexBuilder(Project project) {
        languageServiceLocator = project.getService(LanguageServiceLocator.class);
    }

    public DocumentInfo build(@NotNull PsiFile psiFile) {

        Language language = psiFile.getLanguage();
        LanguageService languageService = languageServiceLocator.locate(language);
        if (!languageService.isIndexedLanguage()) {
            return null;
        }

        return languageService.buildDocumentInfo(psiFile);
    }
}
