package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.Retries;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

//@SuppressWarnings("UnstableApiUsage")
public class JavaEndpointNavigationProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(JavaEndpointNavigationProvider.class);

    private final Map<String, Set<EndpointInfo>> mapEndpointToMethods = new HashMap<>();

    //used to restrict one update thread at a time
    private final ReentrantLock buildEndpointLock = new ReentrantLock();

    private final Project project;

    public JavaEndpointNavigationProvider(Project project) {
        this.project = project;
    }

    public static JavaEndpointNavigationProvider getInstance(@NotNull Project project) {
        return project.getService(JavaEndpointNavigationProvider.class);
    }

    @Override
    public void dispose() {
        //nothing to do
    }

    @NotNull
    public Set<EndpointInfo> getEndpointInfos(String endpointId) {
        var endpointInfos = mapEndpointToMethods.get(endpointId);
        if (endpointInfos == null) {
            return Collections.emptySet();
        }
        // cloning the result, to keep consistency
        return new HashSet<>(endpointInfos);
    }


    public void buildEndpointNavigation() {

        EDT.assertNonDispatchThread();

        Log.log(LOGGER::info, "Building endpoint navigation");


        try {
            buildEndpointLock.lock();
            Retries.simpleRetry(() -> {
                Log.log(LOGGER::info, "Building buildEndpointAnnotations");
                buildEndpointAnnotations(() -> GlobalSearchScope.projectScope(project));
            }, Throwable.class, 100, 5);
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "Exception in buildSpanNavigation buildWithSpanAnnotation");
            ErrorReporter.getInstance().reportError(project, "JavaEndpointNavigationProvider.buildEndpointNavigation.buildEndpointAnnotations", e);
        } finally {
            if (buildEndpointLock.isHeldByCurrentThread()) {
                buildEndpointLock.unlock();
            }
        }


        //todo: is this necessary after the new react app ?
        //trigger a refresh after span navigation is built. this is necessary because the insights and errors
        //views may be populated before span navigation is ready and spans links can not be built.
        //for example is the IDE is closed when the cursor is on a method with Duration Breakdown that
        // has span links, then start the IDE again, the insights view is populated already, without this refresh
        // there will be no links.
        InsightsViewService.getInstance(project).refreshInsightsModel();
        ErrorsViewService.getInstance(project).refreshErrorsModel();
    }


    private void buildEndpointAnnotations(@NotNull Supplier<SearchScope> searchScope) {

        var javaLanguageService = project.getService(JavaLanguageService.class);
        var endpointDiscoveries = javaLanguageService.getListOfEndpointDiscovery();

        endpointDiscoveries.forEach(endpointDiscovery -> {
            try {
                var endpointInfos = Retries.retryWithResult(() ->
                        ProgressManager.getInstance().runProcess(() ->
                                        DumbService.getInstance(project).runReadActionInSmartMode(() ->
                                                endpointDiscovery.lookForEndpoints(searchScope.get())),
                                new EmptyProgressIndicator()), Throwable.class, 50, 5);

                endpointInfos.forEach(this::addToMethodsMap);

            } catch (Exception e) {
                Log.warnWithException(LOGGER, e, "Exception in buildEndpointAnnotations");
                ErrorReporter.getInstance().reportError(project, "JavaEndpointNavigationProvider.buildEndpointAnnotations", e);
            }
        });
    }


    private void buildEndpointAnnotations(@NotNull VirtualFile virtualFile) {

        final PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(virtualFile));
        if (psiFile == null) return; // very unlikely

        var javaLanguageService = project.getService(JavaLanguageService.class);
        var endpointDiscoveries = javaLanguageService.getListOfEndpointDiscovery();

        endpointDiscoveries.forEach(endpointDiscovery -> {

            try {
                var endpointInfos = Retries.retryWithResult(() ->
                        ProgressManager.getInstance().runProcess(() ->
                                        DumbService.getInstance(project).runReadActionInSmartMode(() ->
                                                endpointDiscovery.lookForEndpoints(psiFile)),
                                new EmptyProgressIndicator()), Throwable.class, 50, 5);


                endpointInfos.forEach(this::addToMethodsMap);

            } catch (Exception e) {
                Log.warnWithException(LOGGER, e, "Exception in buildEndpointAnnotations");
                ErrorReporter.getInstance().reportError(project, "JavaEndpointNavigationProvider.buildEndpointAnnotations", e);
            }
        });
    }


    private void addToMethodsMap(@NotNull EndpointInfo endpointInfo) {
        final Set<EndpointInfo> methods = mapEndpointToMethods.computeIfAbsent(endpointInfo.getId(), it -> new HashSet<>());
        methods.add(endpointInfo);
    }


    /*
    This method must be called with a document that is relevant for endpoint discovery and endpoint navigation.
    the tests should be done before calling this method.
     */
    public void documentChanged(@NotNull Document document) {

        try {
            if (project.isDisposed()) {
                return;
            }

            var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile == null || !psiFile.isValid() ||
                    !JavaLanguage.INSTANCE.equals(psiFile.getLanguage())) {
                return;
            }

            var virtualFile = FileDocumentManager.getInstance().getFile(document);
            fileChanged(virtualFile);
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "Exception in documentChanged");
            ErrorReporter.getInstance().reportError(project, "JavaEndpointNavigationProvider.documentChanged", e);
        }
    }


    /*
    This method must be called with a file that is relevant for endpoint discovery and endpoint navigation.
    the tests should be done before calling this method.
     */
    public void fileChanged(VirtualFile virtualFile) {

        if (project.isDisposed()) {
            return;
        }

        Backgroundable.executeOnPooledThread(() -> {
            try {
                if (virtualFile != null && virtualFile.isValid()) {
                    buildEndpointLock.lock();
                    //if file moved then removeDocumentEndpoints will not remove anything but building endpoint locations will
                    // override the entries anyway
                    removeDocumentEndpoint(virtualFile);
                    buildEndpointAnnotations(virtualFile);
                }
            } catch (Exception e) {
                Log.warnWithException(LOGGER, e, "Exception in fileChanged");
                ErrorReporter.getInstance().reportError(project, "JavaEndpointNavigationProvider.fileChanged", e);
            } finally {
                buildEndpointLock.unlock();
            }
        });
    }


    public void fileDeleted(VirtualFile virtualFile) {

        if (project.isDisposed()) {
            return;
        }

        Backgroundable.executeOnPooledThread(() -> {
            if (virtualFile != null) {
                buildEndpointLock.lock();
                try {
                    removeDocumentEndpoint(virtualFile);
                } finally {
                    buildEndpointLock.unlock();
                }
            }
        });
    }


    private void removeDocumentEndpoint(@NotNull VirtualFile virtualFile) {
        var filePredicate = new FilePredicate(virtualFile.getUrl());
        for (Set<EndpointInfo> methods : mapEndpointToMethods.values()) {
            methods.removeIf(filePredicate);
        }
    }

    private static class FilePredicate implements Predicate<EndpointInfo> {

        private final String theFileUri;

        public FilePredicate(String fileUri) {
            this.theFileUri = fileUri;
        }

        @Override
        public boolean test(EndpointInfo endpointInfo) {
            return theFileUri.equals(endpointInfo.getContainingFileUri());
        }
    }

}
