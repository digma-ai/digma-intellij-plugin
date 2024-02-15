package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.Unicodes;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightImportance;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

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

        List<MethodWithCodeObjects> methods = new ArrayList<>();

        for (MethodInfo methodInfo : methodsInfo) {
            List<String> relatedSpansCodeObjectIds = methodInfo.getSpans().stream().map(SpanInfo::getId).toList();
            List<String> relatedEndpointCodeObjectIds = methodInfo.getEndpoints().stream().map(EndpointInfo::getId).toList();

            for (String id : methodInfo.allIdsWithType()) {
                methods.add(new MethodWithCodeObjects(id, relatedSpansCodeObjectIds, relatedEndpointCodeObjectIds));
            }
        }

        var methodsWithCodeLens = analyticsService.getCodeLensByMethods(methods).getMethodWithCodeLens();

        for (MethodWithCodeLens methodWithCodeLens : methodsWithCodeLens) {
            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(methodWithCodeLens.getMethodCodeObjectId());
            var decorators = methodWithCodeLens.getDecorators();

            if (methodWithCodeLens.isAlive()) {
                CodeLens codeLens = buildCodeLensOfActive(codeObjectId);
                codeLensList.add(codeLens);
            }

            var decoratorsGroups = Arrays.stream(decorators).collect(groupingBy(Decorator::getTitle));

            for (Map.Entry<String, List<Decorator>> decoratorsGroup : decoratorsGroups.entrySet()) {
                var groupTitle = decoratorsGroup.getKey();
                var decorator = decoratorsGroup.getValue().stream()
                        .max(Comparator.comparingInt(d -> d.getImportance().getPriority())).get();

                String envComponent = "";
                int importance = decorator.getImportance().getPriority();

                if (environmentPrefix) {
                    envComponent = "[" + environment + "]";
                }

                String priorityEmoji = "";
                if (isImportant(importance)) {
                    priorityEmoji = "❗️";
                }

                String title = priorityEmoji + groupTitle + " " + envComponent;

                CodeLens codeLens = new CodeLens(codeObjectId, title, importance);
                codeLens.setLensDescription(decorator.getDescription());
                codeLens.setLensMoreText("Go to " + title);
                codeLens.setAnchor("Top");

                codeLensList.add(codeLens);
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
