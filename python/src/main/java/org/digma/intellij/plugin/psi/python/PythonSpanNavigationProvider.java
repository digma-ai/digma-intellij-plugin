package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import com.intellij.util.RunnableCallable;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex;
import kotlin.Pair;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.psi.python.PythonCodeObjectsDiscovery.discoverSpanFromStartSpanMethodCallExpression;

public class PythonSpanNavigationProvider implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(PythonSpanNavigationProvider.class);

    private final Map<String, SpanLocation> spanLocations = new HashMap<>();

    //used to restrict one update thread at a time
    private final ReentrantLock buildSpansLock = new ReentrantLock();

    private final Project project;

    public PythonSpanNavigationProvider(Project project) {
        this.project = project;
    }


    public static PythonSpanNavigationProvider getInstance(Project project) {
        return project.getService(PythonSpanNavigationProvider.class);
    }

    @Override
    public void dispose() {
        //nothing to do
    }


    public Map<String, Pair<String, Integer>> getUrisForSpanIds(List<String> spanIds) {
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
                //in python projectScope includes files in venv too. also ProjectFileIndex considers files in venv as project files.
                //method buildSpanNavigation will try to ignore library files when ProjectFileIndex.isInLibrary returns true
                buildSpanNavigation(project,Constants.OPENTELEMETRY_START_AS_CURRENT_SPAN_FUNC_NAME,GlobalSearchScope.projectScope(project));
                buildSpanNavigation(project,Constants.OPENTELEMETRY_START_SPAN_FUNC_NAME,GlobalSearchScope.projectScope(project));

                //trigger a refresh after span navigation is built. this is necessary because the insights and errors
                //views may be populated before span navigation is ready and spans links can not be built.
                //for example is the IDE is closed when the cursor is on a method with Duration Breakdown that
                // has span links, then start the IDE again, the insights view is populated already, without this refresh
                // there will be no links.
                project.getService(InsightsViewService.class).refreshInsightsModel();
                project.getService(ErrorsViewService.class).refreshErrorsModel();

            } catch (Exception e) {
                Log.warnWithException(LOGGER, e, "Exception in buildSpanNavigation");
                ErrorReporter.getInstance().reportError(project, "PythonSpanNavigationProvider.buildSpanNavigation", e);
            } finally {
                buildSpansLock.unlock();
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());
    }



    private void buildSpanNavigation(@NotNull Project project,@NotNull String tracerMethodName,@NotNull SearchScope searchScope) {

        var functions = PyFunctionNameIndex.find(tracerMethodName, project,GlobalSearchScope.allScope(project));

        //for some reason the search returns two identical functions, so just choose the first one.
        // I expect only one and don't know why there are two.
        PyFunction startSpanFunction = functions.stream().filter(pyFunction -> pyFunction.getContainingClass() != null &&
                Constants.OPENTELEMETRY_TRACER_FQN.equals(pyFunction.getContainingClass().getQualifiedName())).findFirst().orElse(null);

        if (startSpanFunction == null) {
            return;
        }

        Query<PsiReference> references = ReferencesSearch.search(startSpanFunction, searchScope);
        references.forEach(psiReference -> {
            Log.log(LOGGER::debug, "found reference to {} function {}", tracerMethodName, psiReference.getElement().getText());
            var pyCallExpression = PsiTreeUtil.getParentOfType(psiReference.getElement(), PyCallExpression.class);
            if (pyCallExpression != null) {
                Log.log(LOGGER::debug, "call expression to {} function is {} ", tracerMethodName, pyCallExpression.getText());
                var pyFile = pyCallExpression.getContainingFile();
                if (PythonLanguageUtils.isProjectFile(project,pyFile)) {
                    var fileUri = PsiUtils.psiFileToUri(pyFile);
                    List<SpanInfo> spanInfos = discoverSpanFromStartSpanMethodCallExpression(project, pyFile, pyCallExpression, fileUri);
                    spanInfos.forEach(spanInfo -> {
                        Log.log(LOGGER::debug, "Found span info {} for method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                        int offset = pyCallExpression.getTextOffset();
                        var location = new SpanLocation(spanInfo.getContainingFileUri(), offset);
                        spanLocations.put(spanInfo.getId(), location);
                    });
                }
            }
        });
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
            if (psiFile == null || !psiFile.isValid() ||
                    !PythonLanguage.INSTANCE.equals(psiFile.getLanguage())) {
                return;
            }

            var virtualFile = FileDocumentManager.getInstance().getFile(document);
            fileChanged(virtualFile);
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "Exception in documentChanged");
            ErrorReporter.getInstance().reportError(project, "PythonSpanNavigationProvider.documentChanged", e);
        }
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
            try {
                if (virtualFile != null && virtualFile.isValid()) {
                    buildSpansLock.lock();
                    try {
                        removeDocumentSpans(virtualFile);
                        buildSpanNavigation(project, Constants.OPENTELEMETRY_START_AS_CURRENT_SPAN_FUNC_NAME, GlobalSearchScope.fileScope(project, virtualFile));
                        buildSpanNavigation(project, Constants.OPENTELEMETRY_START_SPAN_FUNC_NAME, GlobalSearchScope.fileScope(project, virtualFile));
                    } finally {
                        buildSpansLock.unlock();
                    }
                }
            } catch (Exception e) {
                Log.warnWithException(LOGGER, e, "Exception in fileChanged");
                ErrorReporter.getInstance().reportError(project, "PythonSpanNavigationProvider.fileChanged", e);
            }
        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());
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
