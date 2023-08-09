package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.RunnableCallable;
import com.intellij.util.concurrency.NonUrgentExecutor;
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

//@SuppressWarnings("UnstableApiUsage")
public class JavaEndpointNavigationProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(JavaEndpointNavigationProvider.class);

    private final Map<String, Set<EndpointInfo>> mapEndpointToMethods = new HashMap<>();

    //used to restrict one update thread at a time
    private final ReentrantLock buildLock = new ReentrantLock();

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
        ReadAction.nonBlocking(new RunnableCallable(() -> {
            buildLock.lock();
            try {
                Log.log(LOGGER::info, "Building endpoint navigation");
                buildEndpointAnnotations(GlobalSearchScope.projectScope(project), false);

                //trigger a refresh after endpoint navigation is built. this is necessary because the insights and errors
                //views may be populated before endpoint navigation is ready and endpoints links can not be built.
                //for example is the IDE is closed when the cursor is on a method with Duration Breakdown that
                // has endpoint links, then start the IDE again, the insights view is populated already, without this refresh
                // there will be no links.
                project.getService(InsightsViewService.class).refreshInsightsModel();
                project.getService(ErrorsViewService.class).refreshErrorsModel();

            } finally {
                buildLock.unlock();
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());

    }

    private void buildEndpointAnnotations(@NotNull SearchScope searchScope, boolean isFileScope) {
        var javaLanguageService = project.getService(JavaLanguageService.class);
        var endpointDiscoveries = javaLanguageService.getListOfEndpointDiscovery();

        endpointDiscoveries.forEach(endpointDiscovery -> {
            var endpointInfos = endpointDiscovery.lookForEndpoints(searchScope, isFileScope);
            endpointInfos.forEach(endpointInfo -> {
                addToMethodsMap(endpointInfo);
            });
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
    }


    /*
    This method must be called with a file that is relevant for endpoint discovery and endpoint navigation.
    the tests should be done before calling this method.
     */
    public void fileChanged(VirtualFile virtualFile) {

        if (project.isDisposed()) {
            return;
        }

        ReadAction.nonBlocking(new RunnableCallable(() -> {
            if (virtualFile != null && virtualFile.isValid()) {
                buildLock.lock();
                try {
                    //if file moved then removeDocumentEndpoints will not remove anything but building endpoint locations will
                    // override the entries anyway
                    removeDocumentEndpoint(virtualFile);
                    buildEndpointAnnotations(GlobalSearchScope.fileScope(project, virtualFile), true);
                } finally {
                    buildLock.unlock();
                }
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());
    }


    public void fileDeleted(VirtualFile virtualFile) {

        if (project.isDisposed()) {
            return;
        }

        ReadAction.nonBlocking(new RunnableCallable(() -> {
            if (virtualFile != null) {
                buildLock.lock();
                try {
                    removeDocumentEndpoint(virtualFile);
                } finally {
                    buildLock.unlock();
                }
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());
    }

    private void removeDocumentEndpoint(@NotNull VirtualFile virtualFile) {
        var filePredicate = new FilePredicate(virtualFile.getUrl());
        for (Set<EndpointInfo> methods : mapEndpointToMethods.values()) {
            methods.removeIf(filePredicate);
        }
    }

    private static class FilePredicate implements Predicate<EndpointInfo> {

        final private String theFileUri;

        public FilePredicate(String fileUri) {
            this.theFileUri = fileUri;
        }

        @Override
        public boolean test(EndpointInfo endpointInfo) {
            return theFileUri.equals(endpointInfo.getContainingFileUri());
        }
    }

}
