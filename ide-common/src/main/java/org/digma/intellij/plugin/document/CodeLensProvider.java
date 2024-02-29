package org.digma.intellij.plugin.document;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.codelens.CodeLensRefresh;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * get code lens from backend , builds it and caches it while the file is opened.
 * listens to documentInfoChanged and builds the code lens on background.
 * documentInfoChanged will be fired when a file is opened and every time it is changed.
 * when the file is closed the code lens are removed.
 * consumers for code lens never wait for the backend and always get what CodeLensProvider has in its cache.
 * the cache is refreshed every some seconds by CodeLensRefresh that calls refresh on background.
 */
public class CodeLensProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(CodeLensProvider.class);

    private final Project project;

    private final Map<String, DocumentLensPair> codeLensPerFile = new ConcurrentHashMap<>();


    public CodeLensProvider(Project project) {
        this.project = project;
        new CodeLensRefresh(project, this).start();
    }

    @Override
    public void dispose() {
        codeLensPerFile.clear();
    }

    public static CodeLensProvider getInstance(Project project) {
        return project.getService(CodeLensProvider.class);
    }


    private String psiFileToKey(@NotNull PsiFile psiFile) {
        return PsiUtils.psiFileToUri(psiFile);
    }


    @NotNull
    public List<CodeLens> provideCodeLens(@NotNull PsiFile psiFile) {
        var lensPerFile = codeLensPerFile.get(psiFileToKey(psiFile));
        if (lensPerFile == null) {
            return Collections.emptyList();
        }
        return lensPerFile.codeLensList;
    }

    public void clearCodeLens(@NotNull PsiFile psiFile) {
        codeLensPerFile.remove(psiFileToKey(psiFile));
    }


    /**
     * called on background when a file is opened or changed and build code lest for the file.
     * code lens are kept in local cache.
     */
    public void buildCodeLens(@NotNull PsiFile psiFile) throws AnalyticsServiceException {
        try {
            buildCodeLensImpl(psiFile);
        } catch (AnalyticsServiceException e) {
            ErrorReporter.getInstance().reportError("CodeLensProvider.buildCodeLens", e);
        }
    }


    private void buildCodeLensImpl(@NotNull PsiFile psiFile) throws AnalyticsServiceException {

        if (!PsiUtils.isValidPsiFile(psiFile) || !ProjectUtilsKt.isProjectValid(project)) {
            return;
        }

        EDT.assertNonDispatchThread();
        ReadActions.assertNotInReadAccess();

        var psiKey = psiFileToKey(psiFile);

        Log.log(LOGGER::trace, "Got request for code lens for {}", psiKey);

        DocumentInfoContainer documentInfo = DocumentInfoService.getInstance(project).getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::trace, "Can't find DocumentInfo for {}", psiKey);
            codeLensPerFile.put(psiKey, new DocumentLensPair(null, Collections.emptyList()));
        } else {
            var codeLens = buildCodeLens(documentInfo);
            Log.log(LOGGER::trace, "Got code lens for {}, {}", psiKey, codeLens);
            codeLensPerFile.put(psiKey, new DocumentLensPair(documentInfo, codeLens));
        }
    }


    /**
     * called on background by refresh task and when environment changed.
     * refresh code lens for the documents currently in the cache.
     */
    public void refresh() {

        EDT.assertNonDispatchThread();
        ReadActions.assertNotInReadAccess();

        codeLensPerFile.forEach((key, documentLensPair) -> {
            try {
                var documentInfo = documentLensPair.documentInfoContainer;

                if (documentInfo != null) {
                    var codeLens = buildCodeLens(documentInfo);
                    Log.log(LOGGER::trace, "Got code lens for {}, {}", key, codeLens);
                    documentLensPair.codeLensList = codeLens;
                }

            } catch (Throwable e) {
                Log.warnWithException(LOGGER, project, e, "error in code lens refresh");
                ErrorReporter.getInstance().reportError("CodeLensProvider.refresh", e);
            }
        });
    }


    //synchronized to prevent multiple threads building code lens. it all happens on background
    // and should not impact performance of the plugin. consumers of code lens never wait and take what is already
    // in the codeLensPerFile cache.
    @NotNull
    private synchronized List<CodeLens> buildCodeLens(@NotNull DocumentInfoContainer documentInfoContainer) throws AnalyticsServiceException {

        List<CodeLens> codeLensList = new ArrayList<>();

        if (documentInfoContainer.getDocumentInfo() == null) {
            return Collections.emptyList();
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

        var methodsWithCodeLens = AnalyticsService.getInstance(project).getCodeLensByMethods(methods).getMethodWithCodeLens();

        for (MethodWithCodeLens methodWithCodeLens : methodsWithCodeLens) {
            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(methodWithCodeLens.getMethodCodeObjectId());
            var decorators = methodWithCodeLens.getDecorators();

            var liveDecorator =
                    decorators.stream().filter(d -> d.getTitle().equals("Live")).findFirst()
                            .orElse(null);

            if (liveDecorator != null) {
                var codeLens = buildCodeLensOfActive(codeObjectId, liveDecorator);
                decorators.remove(liveDecorator);
                codeLensList.add(codeLens);
            }

            for (Decorator decorator : decorators) {

                int importance = decorator.getImportance().getPriority();

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
        CodeLens codeLens = new CodeLens(methodId, liveDecorator.getCodeObjectId(), title, 1);
        codeLens.setLensDescription(liveDecorator.getDescription());
        codeLens.setAnchor("Top");

        return codeLens;
    }

    private static boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() && importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }


    private static class DocumentLensPair {
        private final @Nullable DocumentInfoContainer documentInfoContainer;
        private @NotNull List<CodeLens> codeLensList;

        public DocumentLensPair(@Nullable DocumentInfoContainer documentInfoContainer, @NotNull List<CodeLens> codeLensList) {
            this.documentInfoContainer = documentInfoContainer;
            this.codeLensList = codeLensList;
        }
    }

}
