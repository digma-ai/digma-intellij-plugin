package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummaryRequest;
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary;
import org.jetbrains.annotations.Nullable;

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
        //todo: maybe always clear methodSummaries and put the new ones. maybe older summaries are not relevant anymore.
        if (summaries.isEmpty()) {
            methodSummaries.clear();
        }else{
            summaries.forEach(codeObjectSummary -> methodSummaries.put(codeObjectSummary.getCodeObjectId(), codeObjectSummary));
        }
    }


    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public Map<String, CodeObjectSummary> getSummaries() {
        //todo: return other summary types
        return methodSummaries;
    }

    public MethodCodeObjectSummary getMethodSummaries(String methodId) {
        return (MethodCodeObjectSummary) methodSummaries.get(methodId);
    }

    @Nullable
    public MethodInfo getMethodInfo(String id) {
        return documentInfo.getMethods().get(id);
    }
}
