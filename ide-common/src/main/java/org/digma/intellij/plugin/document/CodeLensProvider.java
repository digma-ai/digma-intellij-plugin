package org.digma.intellij.plugin.document;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.codelens.CodeLensProviderRefresh;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


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

    private final Map<VirtualFile, DocumentLensPair> codeLensPerFile = new ConcurrentHashMap<>();


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
    public Set<CodeLens> provideCodeLens(@NotNull VirtualFile file) {
        Log.log(LOGGER::trace, "Got request for code lens for {}", file);
        var lensPerFile = codeLensPerFile.get(file);
        if (lensPerFile == null) {
            Log.log(LOGGER::trace, "No lenses for file in cache, returning empty {}", file);
            return Collections.emptySet();
        }
        Log.log(LOGGER::trace, "returning lenses for file {} [{}]", file, lensPerFile.codeLensList);
        return lensPerFile.codeLensList;
    }

    public void clearCodeLens(@NotNull VirtualFile file) {
        Log.log(LOGGER::trace, "clearing code lens for {}", file);
        codeLensPerFile.remove(file);
    }


    /**
     * called on background when a file is opened or changed and build code lest for the file.
     * code lens are kept in local cache.
     * returns true is code lens changed.
     */
    public boolean buildCodeLens(@NotNull VirtualFile file, @NotNull DocumentInfo documentInfo) throws AnalyticsServiceException {
        var oldLenses = provideCodeLens(file);
        buildCodeLensImpl(file, documentInfo);
        var newLenses = provideCodeLens(file);
        return !oldLenses.equals(newLenses);
    }


    private void buildCodeLensImpl(@NotNull VirtualFile file, @NotNull DocumentInfo documentInfo) throws AnalyticsServiceException {

        Log.log(LOGGER::trace, "Building code lens for {}", file);

        if (!VfsUtilsKt.isValidVirtualFile(file) || !ProjectUtilsKt.isProjectValid(project)) {
            return;
        }

        EDT.assertNonDispatchThread();
        ReadActions.assertNotInReadAccess();

        var codeLens = buildCodeLens(documentInfo);
        Log.log(LOGGER::trace, "Built code lens for {}, [{}]", file, codeLens);
        codeLensPerFile.put(file, new DocumentLensPair(documentInfo, codeLens));
    }


    /**
     * called on background by refresh task and when environment changed.
     * refresh code lens for the documents currently in the cache.
     * return the list of psi urls where code lens changed
     */
    public List<VirtualFile> refresh() {

        Log.log(LOGGER::trace, "Got request to refresh code lens");

        EDT.assertNonDispatchThread();
        ReadActions.assertNotInReadAccess();

        var changed = new ArrayList<VirtualFile>();

        codeLensPerFile.forEach((file, documentLensPair) -> {
            try {
                var documentInfo = documentLensPair.documentInfo;
                Log.log(LOGGER::trace, "refreshing code lens for {}", file);
                var oldLenses = documentLensPair.codeLensList;
                var newLenses = buildCodeLens(documentInfo);
                documentLensPair.codeLensList = newLenses;
                if (!oldLenses.equals(newLenses)) {
                    Log.log(LOGGER::trace, "Got refreshed code lens for {}, {}", file, newLenses);
                    changed.add(file);
                }

            } catch (Throwable e) {
                Log.warnWithException(LOGGER, project, e, "error in code lens refresh");
                ErrorReporter.getInstance().reportError(project, "CodeLensProvider.refresh", e);
            }
        });

        return changed;
    }


    //synchronized to prevent multiple threads building code lens. it all happens on background
    // and should not impact performance of the plugin. consumers of code lens never wait and take what is already
    // in the codeLensPerFile cache.
    @NotNull
    private synchronized Set<CodeLens> buildCodeLens(@NotNull DocumentInfo documentInfo) throws AnalyticsServiceException {

        //LinkedHashSet retains insertion order so retains the order from backend
        Set<CodeLens> codeLensList = new LinkedHashSet<>();

        var methodsInfo = documentInfo.getMethods().values();

        List<MethodWithCodeObjects> methods = new ArrayList<>();

        for (MethodInfo methodInfo : methodsInfo) {
            List<String> relatedSpansCodeObjectIds = methodInfo.getSpans().stream().map(SpanInfo::idWithType).toList();
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
                CodeLens codeLens = new CodeLens(decorator.getTitle(), codeObjectId, decorator.getScopeCodeObjectId(), title, importance);
                codeLens.setLensDescription(decorator.getDescription());
                codeLens.setLensMoreText("Go to " + title);

                codeLensList.add(codeLens);
            }

        }

        return codeLensList;
    }

    private static CodeLens buildCodeLensOfActive(String methodId, Decorator liveDecorator) {
        var title = Unicodes.getLIVE_CIRCLE();
        CodeLens codeLens = new CodeLens(liveDecorator.getTitle(), methodId, liveDecorator.getScopeCodeObjectId(), title, 1);
        codeLens.setLensDescription(liveDecorator.getDescription());

        return codeLens;
    }

    private static boolean isImportant(Integer importanceLevel) {
        return importanceLevel <= InsightImportance.HighlyImportant.getPriority() && importanceLevel >= InsightImportance.ShowStopper.getPriority();
    }


    private static class DocumentLensPair {
        private final @NotNull DocumentInfo documentInfo;
        private @NotNull Set<CodeLens> codeLensList;

        public DocumentLensPair(@NotNull DocumentInfo documentInfo, @NotNull Set<CodeLens> codeLensList) {
            this.documentInfo = documentInfo;
            this.codeLensList = codeLensList;
        }
    }

}
