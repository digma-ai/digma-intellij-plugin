package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.CodeObjectType;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeLensProvider {

    private static final Logger LOGGER = Logger.getInstance(CodeLensProvider.class);

    private final Project project;
    private final DocumentInfoService documentInfoService;

    public CodeLensProvider(Project project) {
        this.project = project;
        documentInfoService = project.getService(DocumentInfoService.class);
    }


    public List<CodeLens> provideCodeLens(@NotNull PsiFile psiFile){

        Log.log(LOGGER::debug, "Got request for code lens for {}",psiFile.getVirtualFile());

        List<CodeLens> codeLensList = new ArrayList<>();

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);

        Log.log(LOGGER::debug, "Got DocumentInfo for {}: {}",psiFile.getVirtualFile(),documentInfo.getDocumentInfo());

        Map<String, CodeObjectSummary> summaries = documentInfo.getSummaries();

        summaries.forEach((id, codeObjectSummary) -> {
            switch (codeObjectSummary.getType()){
                case MethodSummary:{
                    MethodCodeObjectSummary methodCodeObjectSummary = (MethodCodeObjectSummary) codeObjectSummary;
                    int score = methodCodeObjectSummary.getScore();
                    if (score >= 70){
                        CodeLens codeLens = new CodeLens(id, CodeObjectType.Method,"Error Hotspot");
                        codeLens.setLensTooltipText("Error Hotspot tooltip");
                        codeLens.setLensMoreText("Error Hotspot more text");
                        codeLens.setAnchor("Top");
                        codeLensList.add(codeLens);
                    }
                }
                case EndpointSummary:{

                }
                case SpanSummary:{

                }
            }

        });

        Log.log(LOGGER::debug, "Collected code lens for {}: {}",psiFile.getVirtualFile(),codeLensList);

        return codeLensList;

    }

}
