package org.digma.intellij.plugin.idea.psi.navigation;

import com.intellij.openapi.Disposable;
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
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.Query;
import kotlin.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.RetryUtilsKt;
import org.digma.intellij.plugin.common.SearchScopeProvider;
import org.digma.intellij.plugin.common.VfsUtilsKt;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.idea.psi.JvmPsiUtilsKt;
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework;
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils;
import org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.PsiAccessUtilsKt;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.DiscoveryConstantsKt.SPAN_BUILDER_FQN;
import static org.digma.intellij.plugin.DiscoveryConstantsKt.WITH_SPAN_ANNOTATION_FQN;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantReferencesForSpanDiscovery;

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

        EDT.assertNonDispatchThread();

        Log.log(LOGGER::info, "Building span navigation");


        try {
            buildSpansLock.lock();
            RetryUtilsKt.runWIthRetry(() -> {
                Log.log(LOGGER::info, "Building buildWithSpanAnnotation");
                buildWithSpanAnnotation(() -> GlobalSearchScope.projectScope(project));
            }, Throwable.class, 100, 5);
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in buildSpanNavigation buildWithSpanAnnotation");
            ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.buildSpanNavigation.buildWithSpanAnnotation", e);
        } finally {
            if (buildSpansLock.isHeldByCurrentThread()) {
                buildSpansLock.unlock();
            }
        }


        try {
            buildSpansLock.lock();
            RetryUtilsKt.runWIthRetry(() -> {
                Log.log(LOGGER::info, "Building buildStartSpanMethodCall");
                buildStartSpanMethodCall(() -> GlobalSearchScope.projectScope(project));
            }, Throwable.class, 100, 5);
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in buildSpanNavigation buildStartSpanMethodCall");
            ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.buildSpanNavigation.buildStartSpanMethodCall", e);
        } finally {
            if (buildSpansLock.isHeldByCurrentThread()) {
                buildSpansLock.unlock();
            }
        }


        try {
            buildSpansLock.lock();
            RetryUtilsKt.runWIthRetry(() -> {
                Log.log(LOGGER::info, "Building buildObservedAnnotation");
                buildObservedAnnotation(() -> GlobalSearchScope.projectScope(project));
            }, Throwable.class, 100, 5);
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in buildSpanNavigation buildObservedAnnotation");
            ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.buildSpanNavigation.buildObservedAnnotation", e);
        } finally {
            if (buildSpansLock.isHeldByCurrentThread()) {
                buildSpansLock.unlock();
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


    //callers to this method should be ready for ProcessCanceledException.
    //the search scope is lastly created. so it will be created inside a read action, file search scope must be created in side read access
    private void buildWithSpanAnnotation(@NotNull SearchScopeProvider searchScopeProvider) {

        if (project.isDisposed() || project.isDefault()) {
            return;
        }

        PsiClass withSpanClass = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () ->
                JavaPsiFacade.getInstance(project).findClass(WITH_SPAN_ANNOTATION_FQN, GlobalSearchScope.allScope(project)));


        //maybe the annotation is not in the classpath
        if (withSpanClass != null) {

            //this section will query for relevant psi methods and return a Collection that can be iterated.
            // iterating directly on Query object may throw a ProcessCanceledException so converting to Collection makes it safer to iterate
            Collection<PsiMethod> psiMethods = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () -> {
                Query<PsiMethod> psiMethodsQuery = AnnotatedElementsSearch.searchPsiMethods(withSpanClass, searchScopeProvider.get());
                psiMethodsQuery = filterNonRelevantMethodsForSpanDiscovery(psiMethodsQuery);
                return psiMethodsQuery.findAll();
            });


            psiMethods.forEach(psiMethod -> {

                //on exception the current method is skipped
                try {

                    List<SpanInfo> spanInfos = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () ->
                            JavaSpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(psiMethod));

                    if (CollectionUtils.isNotEmpty(spanInfos)) {
                        spanInfos.forEach(spanInfo -> PsiAccessUtilsKt.runInReadAccess(() -> {
                            Log.log(LOGGER::debug, "Found span info {} for method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                            int offset = psiMethod.getTextOffset();
                            var location = new SpanLocation(spanInfo.getContainingFileUri(), offset);
                            spanLocations.put(spanInfo.getId(), location);
                        }));
                    }
                } catch (Throwable e) {
                    Log.warnWithException(LOGGER, project, e, "error building span info for method {}", psiMethod.getName());
                    ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.buildSpanNavigation.buildWithSpanAnnotation", e);
                }
            });
        }
    }


    //callers to this method should be ready for ProcessCanceledException.
    //the search scope is lastly created. so it will be created inside a read action, file search scope must be created in side read access
    private void buildStartSpanMethodCall(@NotNull SearchScopeProvider searchScopeProvider) {

        if (project.isDisposed() || project.isDefault()) {
            return;
        }

        PsiClass tracerBuilderClass = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () ->
                JavaPsiFacade.getInstance(project).findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project)));

        if (tracerBuilderClass != null) {

            PsiMethod startSpanMethod = PsiAccessUtilsKt.runInReadAccessWithResult(() -> JavaLanguageUtils.findMethodInClass(tracerBuilderClass, "startSpan", psiMethod -> psiMethod.getParameters().length == 0));
            //must not be null or we have a bug
            Objects.requireNonNull(startSpanMethod, "startSpan method must be found in SpanBuilder class");

            //this section will query for relevant psi references and return a Collection that can be iterated.
            // iterating directly on Query object may throw a ProcessCanceledException so converting to Collection makes it safer to iterate
            Collection<PsiReference> startSpanReferences = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () -> {
                Query<PsiReference> references = MethodReferencesSearch.search(startSpanMethod, searchScopeProvider.get(), true);
                //filter classes that we don't support,which should not happen but just in case. we don't support Annotations,Enums and Records.
                references = filterNonRelevantReferencesForSpanDiscovery(references);
                return references.findAll();
            });


            startSpanReferences.forEach(psiReference -> {

                //on exception the current psiReference is skipped
                try {
                    SpanInfo spanInfo = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () ->
                            JavaSpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, psiReference));

                    if (spanInfo != null) {
                        PsiAccessUtilsKt.runInReadAccess(() -> {
                            Log.log(LOGGER::debug, "Found span info {} in method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                            int lineNumber = psiReference.getElement().getTextOffset();
                            var location = new SpanLocation(spanInfo.getContainingFileUri(), lineNumber);
                            spanLocations.put(spanInfo.getId(), location);
                        });
                    }
                } catch (Throwable e) {
                    Log.warnWithException(LOGGER, project, e, "error building span info for psiReference");
                    ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.buildSpanNavigation.buildStartSpanMethodCall", e);
                }
            });
        }
    }


    //callers to this method should be ready for ProcessCanceledException.
    //the search scope is lastly created. so it will be created inside a read action, file search scope must be created in side read access
    private void buildObservedAnnotation(@NotNull SearchScopeProvider searchScopeProvider) {

        if (project.isDisposed() || project.isDefault()) {
            return;
        }

        PsiClass observedClass = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () ->
                JavaPsiFacade.getInstance(project).findClass(MicrometerTracingFramework.OBSERVED_FQN, GlobalSearchScope.allScope(project)));

        //maybe the annotation is not in the classpath
        if (observedClass != null) {

            //this section will query for relevant psi methods and return a Collection that can be iterated.
            // iterating directly on Query object may throw a ProcessCanceledException so converting to Collection makes it safer to iterate
            Collection<PsiMethod> psiMethods = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () -> {
                Query<PsiMethod> psiMethodsQuery = AnnotatedElementsSearch.searchPsiMethods(observedClass, searchScopeProvider.get());
                psiMethodsQuery = filterNonRelevantMethodsForSpanDiscovery(psiMethodsQuery);
                return psiMethodsQuery.findAll();
            });

            var micrometerTracingFramework = new MicrometerTracingFramework();
            psiMethods.forEach(psiMethod -> {

                //on exception the current psiReference is skipped
                try {

                    var spanInfo = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry(project, () ->
                            micrometerTracingFramework.getSpanInfoFromObservedAnnotatedMethod(psiMethod));

                    if (spanInfo != null) {
                        PsiAccessUtilsKt.runInReadAccess(() -> {
                            Log.log(LOGGER::debug, "Found span info {} for method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                            int offset = psiMethod.getTextOffset();
                            var location = new SpanLocation(spanInfo.getContainingFileUri(), offset);
                            spanLocations.put(spanInfo.getId(), location);
                        });
                    }
                } catch (Throwable e) {
                    Log.warnWithException(LOGGER, project, e, "error building span info for psiMethod {}", psiMethod.getName());
                    ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.buildSpanNavigation.buildObservedAnnotation", e);
                }
            });
        }
    }


    /*
    This method must be called with a document that is relevant for span discovery and span navigation.
    the tests should be done before calling this method.
     */
    public void documentChanged(@NotNull Document document) {

        try {

            if (project.isDisposed()) {
                return;
            }

            var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (!PsiUtils.isValidPsiFile(psiFile) ||
                    !JvmPsiUtilsKt.isJvmSupportedFile(project, psiFile)) {
                return;
            }

            var virtualFile = FileDocumentManager.getInstance().getFile(document);
            if (VfsUtilsKt.isValidVirtualFile(virtualFile)) {
                fileChanged(virtualFile);
            }
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in documentChanged");
            ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.documentChanged", e);
        }
    }


    /*
    This method must be called with a file that is relevant for span discovery and span navigation.
    the tests should be done before calling this method.
     */
    public void fileChanged(VirtualFile virtualFile) {

        if (project.isDisposed() || !VfsUtilsKt.isValidVirtualFile(virtualFile)) {
            return;
        }

        Backgroundable.executeOnPooledThread(() -> {
            try {
                buildSpansLock.lock();
                if (VfsUtilsKt.isValidVirtualFile(virtualFile)) {

                    //if file moved then removeDocumentSpans will not remove anything but building span locations will
                    // override the entries anyway
                    removeDocumentSpans(virtualFile);
                    buildWithSpanAnnotation(() -> GlobalSearchScope.fileScope(project, virtualFile));
                    buildStartSpanMethodCall(() -> GlobalSearchScope.fileScope(project, virtualFile));
                    buildObservedAnnotation(() -> GlobalSearchScope.fileScope(project, virtualFile));
                }
            } catch (Throwable e) {
                Log.warnWithException(LOGGER, e, "Exception in fileChanged");
                ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationProvider.fileChanged", e);
            } finally {
                buildSpansLock.unlock();
            }
        });

    }


    public void fileDeleted(VirtualFile virtualFile) {

        if (project.isDisposed()) {
            return;
        }

        Backgroundable.ensurePooledThread(() -> {
            if (virtualFile != null) {
                buildSpansLock.lock();
                try {
                    removeDocumentSpans(virtualFile);
                } finally {
                    buildSpansLock.unlock();
                }
            }
        });

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
