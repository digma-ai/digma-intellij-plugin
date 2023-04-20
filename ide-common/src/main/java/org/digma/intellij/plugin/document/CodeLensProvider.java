package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.apache.commons.collections4.CollectionUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightImportance;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectDecorator;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CodeLensProvider {

    private static final Logger LOGGER = Logger.getInstance(CodeLensProvider.class);

    private final DocumentInfoService documentInfoService;

    public CodeLensProvider(Project project) {
        documentInfoService = project.getService(DocumentInfoService.class);
    }


    public List<CodeLens> provideCodeLens(@NotNull PsiFile psiFile) {

        Log.log(LOGGER::debug, "Got request for code lens for {}", psiFile.getVirtualFile());

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::debug, "Can't find DocumentInfo for {}", psiFile.getVirtualFile());
            return new ArrayList<>();
        }

        var codeLens = buildCodeLens(documentInfo, false);
        Log.log(LOGGER::debug, "Got code lens for {}, {}", psiFile.getVirtualFile(), codeLens);
        return codeLens;
    }

    private List<CodeLens> buildCodeLens(
            @NotNull DocumentInfoContainer documentInfoContainer,
            boolean environmentPrefix
    ) {
        List<CodeLens> codeLensList = new ArrayList<>();

        var methodsInfo = documentInfoContainer.getDocumentInfo().getMethods().values();

        for (MethodInfo methodInfo : methodsInfo) {
            final var insights = documentInfoContainer.getInsightsForMethod(methodInfo.getId());
            final var hasInsights = CollectionUtils.isNotEmpty(insights);

            if (!hasInsights) {
                if (methodInfo.hasRelatedCodeObjectIds()) {
                    CodeLens codeLen = new CodeLens(methodInfo.getId(), "Never Reached", 7);
                    codeLen.setLensDescription("No tracing data for this code object");
                    codeLen.setAnchor("Top");

                    codeLensList.add(codeLen);
                }
                continue; // to next method
            }

            final boolean haveDecorators = evalHaveDecorators(insights);
            if (!haveDecorators) {
                CodeLens codeLens = new CodeLens(methodInfo.getId(), "Runtime Data", 8);
                codeLens.setLensDescription("Code object has basic insights");
                codeLens.setAnchor("Top");

                codeLensList.add(codeLens);
                continue; // to next method
            }

            for (CodeObjectInsight insight : insights) {
                if (!insight.hasDecorators()) {
                    continue;
                }

                for (CodeObjectDecorator decorator : insight.getDecorators()) {
                    String envComponent = "";
                    if (environmentPrefix) {
                        envComponent = "[" + insight.getEnvironment() + "]";
                    }

                    String priorityEmoji = "";
                    if (isImportant(insight.getImportance())) {
                        priorityEmoji = "❗️";
                    }

                    String title = priorityEmoji + decorator.getTitle() + " " + envComponent;

                    CodeLens codeLens = new CodeLens(methodInfo.getId(), title, insight.getImportance());
                    codeLens.setLensDescription(decorator.getDescription());
                    codeLens.setLensMoreText("Go to " + title);
                    codeLens.setAnchor("Top");

                    codeLensList.add(codeLens);
                }
            }
        } // end of forEach method

        return codeLensList;
    }

    private static boolean evalHaveDecorators(List<CodeObjectInsight> insights) {
        if (CollectionUtils.isEmpty(insights)) {
            return false;
        }

        Optional<CodeObjectInsight> optional = insights.stream()
                .filter(CodeObjectInsight::hasDecorators)
                .findFirst();
        return optional.isPresent();
    }

    private static boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() &&
                importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }

}
