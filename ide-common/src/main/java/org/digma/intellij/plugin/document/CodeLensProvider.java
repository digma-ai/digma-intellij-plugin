package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.Unicodes;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightImportance;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CodeLensProvider {

    private static final Logger LOGGER = Logger.getInstance(CodeLensProvider.class);

    private final DocumentInfoService documentInfoService;
    private final AnalyticsService analyticsService;

    public CodeLensProvider(Project project) {

        documentInfoService = project.getService(DocumentInfoService.class);
        analyticsService = project.getService(AnalyticsService.class);
    }


    @NotNull
    public Set<CodeLens> provideCodeLens(@NotNull PsiFile psiFile) throws AnalyticsServiceException {

        Log.log(LOGGER::trace, "Got request for code lens for {}", psiFile.getVirtualFile());

        DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::debug, "Can't find DocumentInfo for {}", psiFile.getVirtualFile());
            return Collections.emptySet();
        }

        var codeLens = buildCodeLens(documentInfo, false);

        Log.log(LOGGER::trace, "Got code lens for {}, {}", psiFile.getVirtualFile(), codeLens);
        return codeLens;
    }

    @NotNull
    private Set<CodeLens> buildCodeLens(@NotNull DocumentInfoContainer documentInfoContainer, boolean environmentPrefix) throws AnalyticsServiceException {
        Set<CodeLens> codeLensList = new LinkedHashSet<>();

        var environment = analyticsService.getEnvironment();

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

        Set<String> methodWithCodeLensIds = new HashSet<>(methodsWithCodeLens.stream().distinct().map(x -> x.getMethodCodeObjectId()).toList());
        Set<String> requestedMethods = new HashSet<>(methods.stream().distinct().map(x -> x.getCodeObjectId()).toList());

        Set<String> methodsWithoutData = requestedMethods.stream().filter(method -> !methodWithCodeLensIds.contains(method)).collect(Collectors.toSet());

        for (String methodWithoutData : methodsWithoutData) {
            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(methodWithoutData);
            CodeLens codeLen = new CodeLens(codeObjectId, "Never Reached", 7);
            codeLen.setLensDescription("No tracing data for this code object");
            codeLen.setAnchor("Top");
            codeLensList.add(codeLen);
        }

        for (MethodWithCodeLens methodWithCodeLens : methodsWithCodeLens) {
            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(methodWithCodeLens.getMethodCodeObjectId());
            var decorators = methodWithCodeLens.getDecorators();

            if (methodWithCodeLens.isAlive()) {
                CodeLens codeLens = buildCodeLensOfActive(codeObjectId);
                codeLensList.add(codeLens);
            }

            if (decorators.length == 0) {
                CodeLens codeLens = new CodeLens(codeObjectId, "Runtime Data", 8);
                codeLens.setLensDescription("Runtime data available");
                codeLens.setAnchor("Top");

                codeLensList.add(codeLens);
            } else {
                for (Decorator decorator : decorators) {
                    String envComponent = "";
                    Integer importance = decorator.getImportance().getPriority();

                    if (environmentPrefix) {
                        envComponent = "[" + environment + "]";
                    }

                    String priorityEmoji = "";
                    if (isImportant(importance)) {
                        priorityEmoji = "❗️";
                    }

                    String title = priorityEmoji + decorator.getTitle() + " " + envComponent;

                    CodeLens codeLens = new CodeLens(codeObjectId, title, importance);
                    codeLens.setLensDescription(decorator.getDescription());
                    codeLens.setLensMoreText("Go to " + title);
                    codeLens.setAnchor("Top");

                    codeLensList.add(codeLens);
                }
            }
        }

        return codeLensList;
    }

    private static CodeLens buildCodeLensOfActive(String methodId) {
        var title = Unicodes.getLIVE_CIRCLE();
        CodeLens codeLens = new CodeLens(methodId, title, 1);
        codeLens.setLensDescription("Live data available");
        codeLens.setAnchor("Top");

        return codeLens;
    }

    private static boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() && importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }

}
