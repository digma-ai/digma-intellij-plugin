package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.model.DocumentInfo;
import org.digma.intellij.plugin.model.rest.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.CodeObjectSummaryRequest;

import java.util.*;

public class DocumentInfoContainer {


    private final PsiFile psiFile;
    private DocumentInfo documentInfo;
    private final Map<String, CodeObjectSummary> methodSummaries = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoContainer(PsiFile psiFile) {
        this.psiFile = psiFile;
    }


    public void update(DocumentInfo documentInfo, AnalyticsProvider analyticsProvider, String environment) {

        //maybe documentInfo already exists, override it anyway with a new one from analysis
        this.documentInfo = documentInfo;

        List<String> objectIds = new ArrayList<>();
        this.documentInfo.getMethods().forEach((id, methodInfo) -> objectIds.add(methodInfo.idWithType()));

        List<CodeObjectSummary> summaries = analyticsProvider.getSummaries(new CodeObjectSummaryRequest(environment, objectIds));
        summaries.forEach(codeObjectSummary -> methodSummaries.put(codeObjectSummary.getCodeObjectId(), codeObjectSummary));
    }

    public Map<String, CodeObjectSummary> getSummaries() {
        return methodSummaries;
    }
}
