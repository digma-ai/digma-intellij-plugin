package org.digma.intellij.plugin.document;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.*;

/**
 * A container for one document info, it holds the discovery info and the info from the analytics service.
 */
public class DocumentInfoContainer {

    private final Logger LOGGER = Logger.getInstance(DocumentInfoContainer.class);

    private final Language language;

    private DocumentInfo documentInfo;

    //psiUrl is not really necessary, just keep it for information and debugging
    private final String psiUrl;

    public DocumentInfoContainer(@NotNull PsiFile psiFile) {
        psiUrl = PsiUtils.psiFileToUri(psiFile);
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

        Log.log(LOGGER::trace, "Updating document for {}: {}", psiUrl, documentInfo);

        //maybe documentInfo already exists, override it anyway with a new one from analysis
        this.documentInfo = documentInfo;
    }

    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    public String getPsiFileUrl() {
        return psiUrl;
    }

    @Nullable
    public MethodInfo getMethodInfo(String id) {
        return documentInfo.getMethods().get(id);
    }

}
