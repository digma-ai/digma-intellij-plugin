package org.digma.intellij.plugin.document;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.codelens.CodeLensProviderRefresh;
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

import static org.digma.intellij.plugin.document.CodeLensUtils.psiFileToKey;


/**
 * get code lens from backend , builds it and caches it while the file is opened.
 * listens to documentInfoChanged and builds the code lens on background.
 * documentInfoChanged will be fired when a file is opened and every time it is changed.
 * when the file is closed the code lens are removed.
 * consumers for code lens never wait for the backend and always get what CodeLensProvider has in its cache.
 * the cache is refreshed every some seconds by CodeLensProviderRefresh that calls refresh on background.
 */
public class CodeLensProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(CodeLensProvider.class);

    private final Project project;

    private final Map<String, DocumentLensPair> codeLensPerFile = new ConcurrentHashMap<>();


    public CodeLensProvider(Project project) {
        this.project = project;
        //start CodeLensProviderRefresh. CodeLensProviderRefresh is in kotlin so can use coroutines
        new CodeLensProviderRefresh(project, this).start();
    }

    @Override
    public void dispose() {
        codeLensPerFile.clear();
    }

    public static CodeLensProvider getInstance(Project project) {
        return project.getService(CodeLensProvider.class);
    }



    @NotNull
    public Set<CodeLens> provideCodeLens(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::trace, "Got request for code lens for {}", psiFile);
        var lensPerFile = codeLensPerFile.get(psiFileToKey(psiFile));
        if (lensPerFile == null) {
            Log.log(LOGGER::trace, "No lenses for file in cache, returning empty {}", psiFile);
            return Collections.emptySet();
        }
        Log.log(LOGGER::trace, "returning lenses for file {} [{}]", psiFile, lensPerFile.codeLensList);
        return lensPerFile.codeLensList;
    }

    public void clearCodeLens(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::trace, "clearing code lens for {}", psiFile);
        codeLensPerFile.remove(psiFileToKey(psiFile));
    }


    /**
     * called on background when a file is opened or changed and build code lest for the file.
     * code lens are kept in local cache.
     * returns true is code lens changed.
     */
    public boolean buildCodeLens(@NotNull PsiFile psiFile) throws AnalyticsServiceException {
        var oldLenses = provideCodeLens(psiFile);
        buildCodeLensImpl(psiFile);
        var newLenses = provideCodeLens(psiFile);
        return !oldLenses.equals(newLenses);
    }


    private void buildCodeLensImpl(@NotNull PsiFile psiFile) throws AnalyticsServiceException {

        Log.log(LOGGER::trace, "Building code lens for {}", psiFile);

        if (!PsiUtils.isValidPsiFile(psiFile) || !ProjectUtilsKt.isProjectValid(project)) {
            return;
        }

        EDT.assertNonDispatchThread();
        ReadActions.assertNotInReadAccess();

        var psiKey = psiFileToKey(psiFile);

        DocumentInfoContainer documentInfo = DocumentInfoService.getInstance(project).getDocumentInfo(psiFile);
        if (documentInfo == null) {
            Log.log(LOGGER::trace, "Can't find DocumentInfo for {}", psiFile);
            codeLensPerFile.put(psiKey, new DocumentLensPair(null, Collections.emptySet()));
        } else {
            var codeLens = buildCodeLens(documentInfo);
            Log.log(LOGGER::trace, "Built code lens for {}, [{}]", psiFile, codeLens);
            codeLensPerFile.put(psiKey, new DocumentLensPair(documentInfo, codeLens));
        }
    }


    /**
     * called on background by refresh task and when environment changed.
     * refresh code lens for the documents currently in the cache.
     * return the list of psi urls where code lens changed
     */
    public List<String> refresh() {

        Log.log(LOGGER::trace, "Got request to refresh code lens");

        EDT.assertNonDispatchThread();
        ReadActions.assertNotInReadAccess();

        var changed = new ArrayList<String>();

        codeLensPerFile.forEach((key, documentLensPair) -> {
            try {
                var documentInfo = documentLensPair.documentInfoContainer;

                if (documentInfo != null) {
                    Log.log(LOGGER::trace, "refreshing code lens for {}", key);
                    var oldLenses = documentLensPair.codeLensList;
                    var newLenses = buildCodeLens(documentInfo);
                    documentLensPair.codeLensList = newLenses;
                    if (!oldLenses.equals(newLenses)) {
                        Log.log(LOGGER::trace, "Got refreshed code lens for {}, {}", key, newLenses);
                        changed.add(key);
                    }
                }

            } catch (Throwable e) {
                Log.warnWithException(LOGGER, project, e, "error in code lens refresh");
                ErrorReporter.getInstance().reportError("CodeLensProvider.refresh", e);
            }
        });

        return changed;
    }


    //synchronized to prevent multiple threads building code lens. it all happens on background
    // and should not impact performance of the plugin. consumers of code lens never wait and take what is already
    // in the codeLensPerFile cache.
    @NotNull
    private synchronized Set<CodeLens> buildCodeLens(@NotNull DocumentInfoContainer documentInfoContainer) throws AnalyticsServiceException {

        //LinkedHashSet retains insertion order so retains the order from backend
        Set<CodeLens> codeLensList = new LinkedHashSet<>();

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

                //title is used as id of CodeLens
                CodeLens codeLens = new CodeLens(decorator.getTitle(), codeObjectId, decorator.getCodeObjectId(), title, importance);
                codeLens.setLensDescription(decorator.getDescription());
                codeLens.setLensMoreText("Go to " + title);

                codeLensList.add(codeLens);
            }

        }

        return codeLensList;
    }

    private static CodeLens buildCodeLensOfActive(String methodId, Decorator liveDecorator) {
        var title = Unicodes.getLIVE_CIRCLE();
        CodeLens codeLens = new CodeLens(liveDecorator.getTitle(), methodId, liveDecorator.getCodeObjectId(), title, 1);
        codeLens.setLensDescription(liveDecorator.getDescription());

        return codeLens;
    }

    private static boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() && importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }


    private static class DocumentLensPair {
        private final @Nullable DocumentInfoContainer documentInfoContainer;
        private @NotNull Set<CodeLens> codeLensList;

        public DocumentLensPair(@Nullable DocumentInfoContainer documentInfoContainer, @NotNull Set<CodeLens> codeLensList) {
            this.documentInfoContainer = documentInfoContainer;
            this.codeLensList = codeLensList;
        }
    }

}
