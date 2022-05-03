package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.model.MethodInfo;
import org.digma.intellij.plugin.model.rest.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.CodeObjectSummaryRequest;

import java.util.*;

public class DocumentInfoContainer {

    private final PsiFile doc;
    private final Map<String, MethodInfo> methodInfos = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, CodeObjectSummary> methodSummaries = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoContainer(PsiFile doc) {
        this.doc = doc;
    }


    public void addMethods(List<MethodInfo> methodInfos, AnalyticsProvider analyticsProvider, String environment) {
        methodInfos.forEach(methodInfo -> {
            if (!this.methodInfos.containsValue(methodInfo)){
                this.methodInfos.put(methodInfo.getId(),methodInfo);
            }
        });
        List<String> objectIds = new ArrayList<>();
        this.methodInfos.forEach((id, methodInfo) -> {
            objectIds.add(methodInfo.idWithType());
        });

        List<CodeObjectSummary> summaries = analyticsProvider.getSummaries(new CodeObjectSummaryRequest(environment,objectIds));
        summaries.forEach(codeObjectSummary -> {
            methodSummaries.put(codeObjectSummary.getCodeObjectId(),codeObjectSummary);
        });
    }

    public Map<String, CodeObjectSummary> getSummaries() {
        return methodSummaries;
    }
}
