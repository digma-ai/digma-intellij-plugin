package org.digma.intellij.plugin.document;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.codelens.CodeLensRefresh;
import org.digma.intellij.plugin.common.Unicodes;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightImportance;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CodeLensProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(CodeLensProvider.class);

    private final DocumentInfoService documentInfoService;
    private final AnalyticsService analyticsService;

    public CodeLensProvider(Project project) {

        documentInfoService = project.getService(DocumentInfoService.class);
        analyticsService = project.getService(AnalyticsService.class);

        new CodeLensRefresh(project, this).start();
    }

    @Override
    public void dispose() {
        //nothing to do , used as parent disposable
    }

    @NotNull
    public Set<CodeLens> provideCodeLens(@NotNull PsiFile psiFile) throws AnalyticsServiceException {

        Log.log(LOGGER::trace, "Got request for code lens for {}", psiFile.getVirtualFile());

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::debug, "Can't find DocumentInfo for {}", psiFile.getVirtualFile());
            return Collections.emptySet();
        }

        var codeLens = buildCodeLens(documentInfo);

        Log.log(LOGGER::trace, "Got code lens for {}, {}", psiFile.getVirtualFile(), codeLens);
        return codeLens;
    }

    @NotNull
    private Set<CodeLens> buildCodeLens(@NotNull DocumentInfoContainer documentInfoContainer) throws AnalyticsServiceException {
        Set<CodeLens> codeLensList = new LinkedHashSet<>();


        if (documentInfoContainer.getDocumentInfo() == null) {
            return Collections.emptySet();
        }

        var methodsInfo = documentInfoContainer.getDocumentInfo().getMethods().values();

        List<MethodWithCodeObjects> methods = new ArrayList();

        for (MethodInfo methodInfo : methodsInfo) {
            List<String> relatedSpansCodeObjectIds = methodInfo.getSpans().stream().map(x -> x.getId()).toList();
            List<String> relatedEndpointCodeObjectIds = methodInfo.getEndpoints().stream().map(x -> x.getId()).toList();

            for (String id : methodInfo.allIdsWithType()) {
                methods.add(new MethodWithCodeObjects(id, relatedSpansCodeObjectIds, relatedEndpointCodeObjectIds));
            }
        }

        var methodsWithCodeLens = analyticsService.getCodeLensByMethods(methods).getMethodWithCodeLens();

        for (MethodWithCodeLens methodWithCodeLens : methodsWithCodeLens) {
            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(methodWithCodeLens.getMethodCodeObjectId());
            var decorators = methodWithCodeLens.getDecorators();

            var liveDecorator =
                    decorators.stream().filter(d-> d.getTitle().equals("Live")).findFirst()
                            .orElse(null);

            if(liveDecorator != null){
                var codeLens = buildCodeLensOfActive(codeObjectId, liveDecorator);
                decorators.remove(liveDecorator);
                codeLensList.add(codeLens);
            }

            for (Decorator decorator : decorators) {

                Integer importance = decorator.getImportance().getPriority();

                String priorityEmoji = "";
                if (isImportant(importance)) {
                    priorityEmoji = "❗️";
                }

                String title = priorityEmoji + decorator.getTitle();

                CodeLens codeLens = new CodeLens(codeObjectId, decorator.getCodeObjectId(), title, importance);
                codeLens.setLensDescription(decorator.getDescription());
                codeLens.setLensMoreText("Go to " + title);
                codeLens.setAnchor("Top");

                codeLensList.add(codeLens);
            }

        }

        return codeLensList;
    }

    private static CodeLens buildCodeLensOfActive(String methodId, Decorator liveDecorator) {
        var title = Unicodes.getLIVE_CIRCLE();
        CodeLens codeLens = new CodeLens(methodId,liveDecorator.getCodeObjectId(), title, 1);
        codeLens.setLensDescription(liveDecorator.getDescription());
        codeLens.setAnchor("Top");

        return codeLens;
    }

    private static boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() && importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }


}
