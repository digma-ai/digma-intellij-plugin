package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
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
    private final Project project;

    public CodeLensProvider(Project project) {
        this.project = project;
        documentInfoService = project.getService(DocumentInfoService.class);
    }


    /*
    JavaCodeLensService may call for code lens before the document info is available.
    it may happen because the daemon may run before we process the editor selectionChange event.
    in that case we don't want to report an error. the daemon will run again after we process
     the editor selectionChange event and then document info will be available
     */
    public List<CodeLens> provideCodeLensNoError(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "Got request for code lens for {}", psiFile.getVirtualFile());

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::debug, "Can't find DocumentInfo for {}", psiFile.getVirtualFile());
            return new ArrayList<>();
        }

        return buildCodeLens(documentInfo, false);
    }


    public List<CodeLens> provideCodeLens(@NotNull PsiFile psiFile) {

        Log.log(LOGGER::debug, "Got request for code lens for {}", psiFile.getVirtualFile());

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::debug, "Can't find DocumentInfo for {}", psiFile.getVirtualFile());
            return new ArrayList<>();
        }

        return buildCodeLens(documentInfo, false);
    }

    private List<CodeLens> buildCodeLens(
            @NotNull DocumentInfoContainer documentInfo,
            @NotNull boolean environmentPrefix
    ) {
        List<CodeLens> codeLensList = new ArrayList<>();

        List<CodeObjectInsight> insightsList = documentInfo.getAllInsights();

        insightsList.forEach(insight -> {
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

                            CodeLens codeLen = new CodeLens(insight.getCodeObjectId(), title, insight.getImportance());
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

    private boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() &&
                importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }

}
