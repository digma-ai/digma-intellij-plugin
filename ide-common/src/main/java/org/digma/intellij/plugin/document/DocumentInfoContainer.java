package org.digma.intellij.plugin.document;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import org.digma.intellij.plugin.common.PsiAccessUtilsKt;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.jetbrains.annotations.*;

/**
 * A container for one document info, it holds the discovery info and the info from the analytics service.
 */
public class DocumentInfoContainer {

    private final Logger LOGGER = Logger.getInstance(DocumentInfoContainer.class);

    //todo: remove , not in use anymore
    private final SmartPsiElementPointer<PsiFile> psiFilePointer;

    private final Language language;

    private DocumentInfo documentInfo;

    public DocumentInfoContainer(@NotNull PsiFile psiFile) {
        this.psiFilePointer = PsiAccessUtilsKt.runInReadAccessWithResult(() -> SmartPointerManager.createPointer(psiFile));
        language = psiFile.getLanguage();
    }

    public Language getLanguage() {
        return language;
    }

    /**
     * update is invoked every time new code objects are available. usually when a document is opened in
     * the editor and every time the document changes.
     * (document change events are fired only after some quite period, see for example the grouping event in
     * Digma.Rider.Discovery.EditorListener.DocumentChangeTracker)
     */
    public void update(@NotNull DocumentInfo documentInfo) {

        Log.log(LOGGER::trace, "Updating document for {}: {}", psiFilePointer.getVirtualFile(), documentInfo);

        //maybe documentInfo already exists, override it anyway with a new one from analysis
        this.documentInfo = documentInfo;
    }

    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    //todo: remove , not in use anymore
    public PsiFile getPsiFile() {
        return psiFilePointer.getElement();
    }

    @Nullable
    public MethodInfo getMethodInfo(String id) {
        return documentInfo.getMethods().get(id);
    }

}
