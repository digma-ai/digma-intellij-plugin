package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.Query;
import com.intellij.util.RunnableCallable;
import com.intellij.util.concurrency.NonUrgentExecutor;
import kotlin.Pair;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.idea.psi.java.Constants.SPAN_BUILDER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.SpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery;
import static org.digma.intellij.plugin.idea.psi.java.SpanDiscoveryUtils.filterNonRelevantReferencesForSpanDiscovery;

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
                buildWithSpanAnnotation();
                buildStartSpanMethodCall();
            } finally {
                buildSpansLock.unlock();
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());

    }


    private void buildStartSpanMethodCall() {
        PsiClass tracerBuilderClass = JavaPsiFacade.getInstance(project).findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project));
        if (tracerBuilderClass != null) {
            PsiMethod startSpanMethod =
                    JavaLanguageUtils.findMethodInClass(tracerBuilderClass, "startSpan", psiMethod -> psiMethod.getParameters().length == 0);
            Objects.requireNonNull(startSpanMethod, "startSpan method must be found in SpanBuilder class");

            Query<PsiReference> startSpanReferences = MethodReferencesSearch.search(startSpanMethod, GlobalSearchScope.projectScope(project), true);
            //filter classes that we don't support,which should not happen but just in case. we don't support Annotations,Enums and Records.
            startSpanReferences = filterNonRelevantReferencesForSpanDiscovery(startSpanReferences);

            startSpanReferences.forEach(psiReference -> {
                SpanInfo spanInfo = SpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, psiReference);
                if (spanInfo != null) {
                    int lineNumber = psiReference.getElement().getTextOffset();
                    var location = new SpanLocation(spanInfo.getContainingFileUri(), lineNumber);
                    spanLocations.put(spanInfo.getId(), location);
                }
            });
        }
    }


    private void buildWithSpanAnnotation() {
        PsiClass withSpanClass = JavaPsiFacade.getInstance(project).findClass(Constants.WITH_SPAN_FQN, GlobalSearchScope.allScope(project));
        //maybe the annotation is not in the classpath
        if (withSpanClass != null) {
            Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(withSpanClass, GlobalSearchScope.projectScope(project));
            psiMethods = filterNonRelevantMethodsForSpanDiscovery(psiMethods);
            psiMethods.forEach(psiMethod -> {
                SpanInfo spanInfo = SpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(psiMethod);
                if (spanInfo != null) {
                    int offset = psiMethod.getTextOffset();
                    var location = new SpanLocation(spanInfo.getContainingFileUri(), offset);
                    spanLocations.put(spanInfo.getId(), location);
                }
            });
        }
    }


    public void documentChanged(@NotNull Document document) {

        if (project.isDisposed()) {
            return;
        }

        var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null || !psiFile.isValid() ||
                !JavaLanguage.INSTANCE.equals(psiFile.getLanguage())) {
            return;
        }

        ReadAction.nonBlocking(new RunnableCallable(() -> {
            buildSpansLock.lock();
            try {
                processDocumentChange(document);
            } finally {
                buildSpansLock.unlock();
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());
    }



    private void processDocumentChange(@NotNull Document document){
        var virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile != null && virtualFile.isValid()) {
            buildSpansLock.lock();
            try {
                removeDocumentSpans(virtualFile);
                buildWithSpanAnnotation(virtualFile);
                buildStartSpanMethodCall(virtualFile);
            }finally {
                buildSpansLock.unlock();
            }
        }
    }


    private void removeDocumentSpans(@NotNull VirtualFile virtualFile) {
        //find all spans that are in virtualFile
        var fileSpans = spanLocations.entrySet().stream().
                filter(stringSpanLocationEntry -> stringSpanLocationEntry.getValue().fileUri.equals(virtualFile.getUrl())).
                map(Map.Entry::getKey).collect(Collectors.toSet());

        //remove all spans for virtualFile
        fileSpans.forEach(spanLocations::remove);
    }

    private void buildStartSpanMethodCall(VirtualFile virtualFile) {
        PsiClass tracerBuilderClass = JavaPsiFacade.getInstance(project).findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project));
        if (tracerBuilderClass != null) {
            PsiMethod startSpanMethod =
                    JavaLanguageUtils.findMethodInClass(tracerBuilderClass, "startSpan", psiMethod -> psiMethod.getParameters().length == 0);
            Objects.requireNonNull(startSpanMethod, "startSpan method must be found in SpanBuilder class");

            Query<PsiReference> startSpanReferences = MethodReferencesSearch.search(startSpanMethod, GlobalSearchScope.fileScope(project, virtualFile), true);
            //filter classes that we don't support,which should not happen but just in case. we don't support Annotations,Enums and Records.
            startSpanReferences = filterNonRelevantReferencesForSpanDiscovery(startSpanReferences);

            startSpanReferences.forEach(psiReference -> {
                SpanInfo spanInfo = SpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, psiReference);
                if (spanInfo != null) {
                    int lineNumber = psiReference.getElement().getTextOffset();
                    var location = new SpanLocation(spanInfo.getContainingFileUri(), lineNumber);
                    spanLocations.put(spanInfo.getId(), location);
                }
            });
        }
    }

    private void buildWithSpanAnnotation(VirtualFile virtualFile) {

        PsiClass withSpanClass = JavaPsiFacade.getInstance(project).findClass(Constants.WITH_SPAN_FQN, GlobalSearchScope.allScope(project));
        //maybe the annotation is not in the classpath
        if (withSpanClass != null) {
            Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(withSpanClass, GlobalSearchScope.fileScope(project, virtualFile));
            psiMethods = filterNonRelevantMethodsForSpanDiscovery(psiMethods);
            psiMethods.forEach(psiMethod -> {
                SpanInfo spanInfo = SpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(psiMethod);
                if (spanInfo != null) {
                    int offset = psiMethod.getTextOffset();
                    var location = new SpanLocation(spanInfo.getContainingFileUri(), offset);
                    spanLocations.put(spanInfo.getId(), location);
                }
            });
        }
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
