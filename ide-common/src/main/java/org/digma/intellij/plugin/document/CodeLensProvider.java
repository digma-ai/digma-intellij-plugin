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
import org.digma.intellij.plugin.notifications.NotificationUtil;
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
    public List<CodeLens> provideCodeLensNoError(@NotNull PsiFile psiFile){
        Log.log(LOGGER::debug, "Got request for code lens for {}", psiFile.getVirtualFile());

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::debug, "Can't find DocumentInfo for {}", psiFile.getVirtualFile());
            return new ArrayList<>();
        }

        return buildCodeLens(psiFile,documentInfo);
    }


    public List<CodeLens> provideCodeLens(@NotNull PsiFile psiFile) {

        Log.log(LOGGER::debug, "Got request for code lens for {}", psiFile.getVirtualFile());

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::error, "Can't find DocumentInfo for {}", psiFile.getVirtualFile());
            return new ArrayList<>();
        }

        return buildCodeLens(psiFile,documentInfo);
    }

    private List<CodeLens> buildCodeLens(@NotNull PsiFile psiFile,@NotNull DocumentInfoContainer documentInfo) {

        Log.log(LOGGER::debug, "Got DocumentInfo for {}: {}",psiFile.getVirtualFile(),documentInfo.getDocumentInfo());

        List<CodeLens> codeLensList = new ArrayList<>();

        List<CodeObjectSummary> summaries = documentInfo.getAllSummaries();

        summaries.forEach(codeObjectSummary -> {
            switch (codeObjectSummary.getType()){
                case MethodSummary:{

                    MethodCodeObjectSummary methodCodeObjectSummary = (MethodCodeObjectSummary) codeObjectSummary;
                    int score = methodCodeObjectSummary.getScore();
                    if (score >= 70){
                        Log.log(LOGGER::debug, "Collecting code lese for MethodCodeObjectSummary {}",codeObjectSummary.getCodeObjectId());
                        CodeLens codeLens = new CodeLens(codeObjectSummary.getCodeObjectId(), CodeObjectType.Method, CodeLens.CodeLensType.ErrorHotspot, "Error Hotspot");
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
                    if (endpointCodeObjectSummary.getLowUsage() || endpointCodeObjectSummary.getHighUsage()) {
                        Log.log(LOGGER::debug, "Collecting code lese for EndpointCodeObjectSummary {}", codeObjectSummary.getCodeObjectId());
                        var lensText = endpointCodeObjectSummary.getLowUsage() ? "Low Usage" : "High Usage";
                        var lensType = endpointCodeObjectSummary.getLowUsage() ? CodeLens.CodeLensType.LowUsage : CodeLens.CodeLensType.HighUsage;
                        CodeLens codeLens = new CodeLens(codeObjectSummary.getCodeObjectId(), CodeObjectType.Method, lensType, lensText);
                        codeLens.setLensTooltipText("Maximum of " + endpointCodeObjectSummary.getMaxCallsIn1Min() + " requests per minute");
                        codeLens.setLensMoreText("Go to " + lensText);
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
