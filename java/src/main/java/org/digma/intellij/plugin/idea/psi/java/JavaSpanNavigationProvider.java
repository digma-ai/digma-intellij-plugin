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
import com.intellij.util.Alarm;
import com.intellij.util.AlarmFactory;
import com.intellij.util.Query;
import kotlin.Pair;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.idea.psi.java.Constants.SPAN_BUILDER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.SpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery;
import static org.digma.intellij.plugin.idea.psi.java.SpanDiscoveryUtils.filterNonRelevantReferencesForSpanDiscovery;

@SuppressWarnings("UnstableApiUsage")
public class JavaSpanNavigationProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(JavaSpanNavigationProvider.class);

    private final Map<String, SpanLocation> spanLocations = new HashMap<>();

    private final Set<Document> changedDocuments = Collections.synchronizedSet(new HashSet<>());

    private final Alarm documentChangeAlarm;

    //used to restrict one update thread at a time
    private final ReentrantLock documentUpdateLock = new ReentrantLock();

    private final Project project;

    public JavaSpanNavigationProvider(Project project) {
        this.project = project;
        documentChangeAlarm = AlarmFactory.getInstance().create();
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


    public void build() {
        Backgroundable.ensureBackground(project, "Build Span Navigation", () -> ReadAction.run(() -> {
            buildWithSpanAnnotation();
            buildStartSpanMethodCall();
        }));
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

        try {
            var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile == null || !psiFile.isValid() ||
                    !JavaLanguage.INSTANCE.equals(psiFile.getLanguage())) {
                return;
            }
        }catch (Throwable e){
            Log.log(LOGGER::debug, "could not find psi file for document {}", document);
            return;
        }


        //simple locking, should not be a problem, there are not so many threads working here.
        //It's only for copying the set when the update operation starts.
        //synchronized block is the simplest way here assuming there are not many threads, usually documents are added
        // in EDT and read when the update operation start.
        synchronized (changedDocuments) {
            changedDocuments.add(document);
        }

        documentChangeAlarm.cancelAllRequests();
        documentChangeAlarm.addRequest(this::updateChangedDocuments, 30000);
    }


    private void updateChangedDocuments() {

        PsiDocumentManager.getInstance(project).
                performLaterWhenAllCommitted(() -> Backgroundable.ensureBackground(project, "Update Span Navigation", () -> {

            //lock the changedDocuments for very short time
            Set<Document> documentsToUpdate;
            synchronized (changedDocuments) {
                documentsToUpdate = new HashSet<>(changedDocuments);
                changedDocuments.clear();
            }

            ReadAction.run(() -> {

                //prevent more than one update task at a time
                documentUpdateLock.lock();
                try {
                    documentsToUpdate.forEach(document -> {
                        var virtualFile = FileDocumentManager.getInstance().getFile(document);
                        if (virtualFile != null && virtualFile.isValid()) {
                            removeDocumentSpans(virtualFile);
                            buildWithSpanAnnotation(virtualFile);
                            buildStartSpanMethodCall(virtualFile);
                        }
                    });
                } finally {
                    documentUpdateLock.unlock();
                }
            });
        }));
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
