package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang3.StringUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightImportance;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectDecorator;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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

        List<CodeObjectInsight> methodInsightsList = documentInfoContainer.getDocumentInfo().getMethods().values().stream()
                .flatMap(methodInfo -> documentInfoService.getCachedMethodInsights(methodInfo).stream())
                .toList();

        methodInsightsList.forEach(insight -> {
                    if (insight.getDecorators() != null && insight.getDecorators().size() > 0) {
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

                            CodeLens codeLen = new CodeLens(getMethodCodeObjectId(insight), title, insight.getImportance());
                            codeLen.setLensDescription(decorator.getDescription());
                            codeLen.setLensMoreText("Go to " + title);
                            codeLen.setAnchor("Top");

                            codeLensList.add(codeLen);
                        }
                    }
                }
        );
        return codeLensList;
    }

    private String getMethodCodeObjectId(CodeObjectInsight insight) {
        if (StringUtils.isNotEmpty(insight.getPrefixedCodeObjectId())) {
            return insight.getPrefixedCodeObjectId().replace("method:", "");
        } else {
            return insight.getCodeObjectId();
        }
    }

    private boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() &&
                importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }

}
