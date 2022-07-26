package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.CodeObjectType;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.EndpointCodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CodeLensProvider {

    private static final Logger LOGGER = Logger.getInstance(CodeLensProvider.class);

    private final DocumentInfoService documentInfoService;

    public CodeLensProvider(Project project) {
        documentInfoService = project.getService(DocumentInfoService.class);
    }


    public List<CodeLens> provideCodeLens(@NotNull PsiFile psiFile){

        Log.log(LOGGER::debug, "Got request for code lens for {}",psiFile.getVirtualFile());

        List<CodeLens> codeLensList = new ArrayList<>();

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null){
            Log.log(LOGGER::error, "Can't find DocumentInfo for {}",psiFile.getVirtualFile());
            return new ArrayList<>();
        }

        Log.log(LOGGER::debug, "Got DocumentInfo for {}: {}",psiFile.getVirtualFile(),documentInfo.getDocumentInfo());

        List<CodeObjectSummary> summaries = documentInfo.getAllSummaries();

        summaries.forEach(codeObjectSummary -> {
            switch (codeObjectSummary.getType()){
                case MethodSummary:{
                    MethodCodeObjectSummary methodCodeObjectSummary = (MethodCodeObjectSummary) codeObjectSummary;
                    int score = methodCodeObjectSummary.getScore();
                    if (score >= 70){
                        Log.log(LOGGER::debug, "Collecting code lese for MethodCodeObjectSummary {}",codeObjectSummary.getCodeObjectId());
                        CodeLens codeLens = new CodeLens(codeObjectSummary.getCodeObjectId(), CodeObjectType.Method,"Error Hotspot");
                        codeLens.setLensTooltipText("Error Hotspot for "+codeObjectSummary.getCodeObjectId());
                        codeLens.setLensMoreText("Go to Error Hotspot");
                        codeLens.setAnchor("Top");
                        codeLensList.add(codeLens);
                    }else{
                        Log.log(LOGGER::debug, "Not Collecting code lese for {} because score is less the 70",codeObjectSummary.getCodeObjectId());
                    }
                    break;
                }
                case EndpointSummary:{
                    EndpointCodeObjectSummary endpointCodeObjectSummary = (EndpointCodeObjectSummary) codeObjectSummary;
                    if (endpointCodeObjectSummary.getLowUsage() || endpointCodeObjectSummary.getHighUsage()){
                        Log.log(LOGGER::debug, "Collecting code lese for EndpointCodeObjectSummary {}",codeObjectSummary.getCodeObjectId());
                        var lensText = endpointCodeObjectSummary.getLowUsage() ? "Low Usage" : "High Usage";
                        CodeLens codeLens = new CodeLens(codeObjectSummary.getCodeObjectId(), CodeObjectType.Method,lensText);
                        codeLens.setLensTooltipText("Maximum of "+endpointCodeObjectSummary.getMaxCallsIn1Min() +" requests per minute");
                        codeLens.setLensMoreText("Go to "+lensText);
                        codeLens.setAnchor("Top");
                        codeLensList.add(codeLens);
                    }
                    break;
                }
                case SpanSummary:{

                    break;
                }
            }});


        Log.log(LOGGER::debug, "Collected code lens for {}: {}",psiFile.getVirtualFile(),codeLensList);

        return codeLensList;

    }

}
