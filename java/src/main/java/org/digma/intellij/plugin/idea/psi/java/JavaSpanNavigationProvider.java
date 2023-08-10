package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.Query;
import com.intellij.util.RunnableCallable;
import com.intellij.util.concurrency.NonUrgentExecutor;
import kotlin.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.idea.psi.java.Constants.SPAN_BUILDER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantReferencesForSpanDiscovery;

@SuppressWarnings("UnstableApiUsage")
public class JavaSpanNavigationProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(JavaSpanNavigationProvider.class);

    private final Map<String, SpanLocation> spanLocations = new HashMap<>();

    //used to restrict one update thread at a time
    private final ReentrantLock buildSpansLock = new ReentrantLock();

    private final Project project;

    public JavaSpanNavigationProvider(Project project) {
        this.project = project;
    }

    public static JavaSpanNavigationProvider getInstance(@NotNull Project project) {
        Log.log(LOGGER::warn, "Getting instance of " + JavaSpanNavigationProvider.class.getSimpleName());
        return project.getService(JavaSpanNavigationProvider.class);
    }

    @Override
    public void dispose() {
        //nothing to do
    }


    @NotNull
    public Map<String, Pair<String, Integer>> getUrisForSpanIds(@NotNull List<String> spanIds) {

        Map<String, Pair<String, Integer>> workspaceUris = new HashMap<>();

        spanIds.forEach(id -> {
            var spanLocation = spanLocations.get(id);
            if (spanLocation != null) {
                workspaceUris.put(id, new Pair<>(spanLocation.fileUri, spanLocation.offset));
            }
        });

        return workspaceUris;
    }


    public void buildSpanNavigation() {
        ReadAction.nonBlocking(new RunnableCallable(() -> {
            buildSpansLock.lock();
            try {
                Log.log(LOGGER::info, "Building span navigation");
                buildWithSpanAnnotation(GlobalSearchScope.projectScope(project));
                buildStartSpanMethodCall(GlobalSearchScope.projectScope(project));

                buildObservedAnnotation(GlobalSearchScope.projectScope(project));

                //trigger a refresh after span navigation is built. this is necessary because the insights and errors
                //views may be populated before span navigation is ready and spans links can not be built.
                //for example is the IDE is closed when the cursor is on a method with Duration Breakdown that
                // has span links, then start the IDE again, the insights view is populated already, without this refresh
                // there will be no links.
                project.getService(InsightsViewService.class).refreshInsightsModel();
                project.getService(ErrorsViewService.class).refreshErrorsModel();

            } finally {
                buildSpansLock.unlock();
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());

    }


    private void buildStartSpanMethodCall(@NotNull SearchScope searchScope) {
        PsiClass tracerBuilderClass = JavaPsiFacade.getInstance(project).findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project));
        if (tracerBuilderClass != null) {
            PsiMethod startSpanMethod =
                    JavaLanguageUtils.findMethodInClass(tracerBuilderClass, "startSpan", psiMethod -> psiMethod.getParameters().length == 0);
            Objects.requireNonNull(startSpanMethod, "startSpan method must be found in SpanBuilder class");

            Query<PsiReference> startSpanReferences = MethodReferencesSearch.search(startSpanMethod, searchScope, true);
            //filter classes that we don't support,which should not happen but just in case. we don't support Annotations,Enums and Records.
            startSpanReferences = filterNonRelevantReferencesForSpanDiscovery(startSpanReferences);

            startSpanReferences.forEach(psiReference -> {
                SpanInfo spanInfo = JavaSpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, psiReference);
                if (spanInfo != null) {
                    Log.log(LOGGER::debug, "Found span info {} in method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                    int lineNumber = psiReference.getElement().getTextOffset();
                    var location = new SpanLocation(spanInfo.getContainingFileUri(), lineNumber);
                    spanLocations.put(spanInfo.getId(), location);
                }
            });
        }
    }


    private void buildWithSpanAnnotation(@NotNull SearchScope searchScope) {
        PsiClass withSpanClass = JavaPsiFacade.getInstance(project).findClass(Constants.WITH_SPAN_FQN, GlobalSearchScope.allScope(project));
        //maybe the annotation is not in the classpath
        if (withSpanClass != null) {
            Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(withSpanClass, searchScope);
            psiMethods = filterNonRelevantMethodsForSpanDiscovery(psiMethods);
            psiMethods.forEach(psiMethod -> {
                List<SpanInfo> spanInfos = JavaSpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(psiMethod);
                if (CollectionUtils.isNotEmpty(spanInfos)) {
                    spanInfos.forEach(spanInfo -> {
                        Log.log(LOGGER::debug, "Found span info {} for method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                        int offset = psiMethod.getTextOffset();
                        var location = new SpanLocation(spanInfo.getContainingFileUri(), offset);
                        spanLocations.put(spanInfo.getId(), location);
                    });
                }
            });
        }
    }

    private void buildObservedAnnotation(@NotNull SearchScope searchScope) {
        PsiClass observedClass = JavaPsiFacade.getInstance(project).findClass(MicrometerTracingFramework.OBSERVED_FQN, GlobalSearchScope.allScope(project));
        //maybe the annotation is not in the classpath
        if (observedClass != null) {
            var micrometerTracingFramework = new MicrometerTracingFramework(project);
            Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(observedClass, searchScope);
            psiMethods = filterNonRelevantMethodsForSpanDiscovery(psiMethods);
            psiMethods.forEach(psiMethod -> {
                var spanInfo = micrometerTracingFramework.getSpanInfoFromObservedAnnotatedMethod(psiMethod);
                if (spanInfo != null) {
                    Log.log(LOGGER::debug, "Found span info {} for method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                    int offset = psiMethod.getTextOffset();
                    var location = new SpanLocation(spanInfo.getContainingFileUri(), offset);
                    spanLocations.put(spanInfo.getId(), location);
                }
            });
        }
    }


    /*
    This method must be called with a document that is relevant for span discovery and span navigation.
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
    This method must be called with a file that is relevant for span discovery and span navigation.
    the tests should be done before calling this method.
     */
    public void fileChanged(VirtualFile virtualFile) {

        if (project.isDisposed()) {
            return;
        }

        ReadAction.nonBlocking(new RunnableCallable(() -> {
            if (virtualFile != null && virtualFile.isValid()) {
                buildSpansLock.lock();
                try {
                    //if file moved then removeDocumentSpans will not remove anything but building span locations will
                    // override the entries anyway
                    removeDocumentSpans(virtualFile);
                    buildWithSpanAnnotation(GlobalSearchScope.fileScope(project, virtualFile));
                    buildStartSpanMethodCall(GlobalSearchScope.fileScope(project, virtualFile));

                    buildObservedAnnotation(GlobalSearchScope.fileScope(project, virtualFile));
                } finally {
                    buildSpansLock.unlock();
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
                buildSpansLock.lock();
                try {
                    removeDocumentSpans(virtualFile);
                } finally {
                    buildSpansLock.unlock();
                }
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());
    }

    private void removeDocumentSpans(@NotNull VirtualFile virtualFile) {
        //find all spans that are in virtualFile
        var fileSpans = spanLocations.entrySet().stream().
                filter(stringSpanLocationEntry -> stringSpanLocationEntry.getValue().fileUri.equals(virtualFile.getUrl())).
                map(Map.Entry::getKey).collect(Collectors.toSet());

        //remove all spans for virtualFile
        fileSpans.forEach(spanLocations::remove);
    }


    private static class SpanLocation {

        private final String fileUri;
        private final int offset;

        public SpanLocation(String fileUri, int offset) {
            this.fileUri = fileUri;
            this.offset = offset;
        }
    }

}
