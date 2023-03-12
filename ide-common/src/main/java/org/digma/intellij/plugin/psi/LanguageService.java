package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface LanguageService extends Disposable {

    Logger LOGGER = Logger.getInstance(LanguageService.class);



    @Override
    default void dispose() {

    }


    /**
     * Some language services need to ensure some startup activity to be made on EDT.
     * C# language service needs to initialize the protocol models on EDT and is the main reason for this method.
     * although in Rider there is a ServicesStarter that is a StartupActivity, some language service methods may be
     * called before StartupActivity, usually when documents are automatically opened on startup, documents that were
     * opened on shutdown.
     * This method must be called from EDT, it will not call invokeLater and shouldn't do it.
     * This method should not be called often, it is a startup task, it is called when the tool window is created
     * and once from EditorEventsHandler.selectionChanged.
     * the language services should be ready for this method to be called few times during startup and should not do
     * long-running tasks, or tasks that can not be performed more than once.
     * as said it is mainly for C# language service to initialize the protocol models on EDT.
     */
    static void ensureStartupOnEDTForAll(@NotNull Project project) {
        Log.log(LOGGER::debug, "ensureStartupOnEDTForAll invoked");
        EDT.assertEDT("ensureStartupOnEDTForAll must be invoked on EDT");
        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                @SuppressWarnings("unchecked")
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                if (languageService != null) {
                    Log.log(LOGGER::debug, "calling ensureStartupOnEDT for {}", languageService);
                    languageService.ensureStartupOnEDT(project);
                }
            } catch (Exception e) {
                Log.debugWithException(LOGGER,e,"exception in ensureStartupOnEDTForAll {}",e.getMessage());
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //JavaLanguageService will fail to load on rider, etc.
            }
        }

    }

    void ensureStartupOnEDT(@NotNull Project project);


    /**
     * Used for running a task in smart mode.
     * Usually this is meant for startup initialization tasks that should run only in smart mode.
     * generally in intellij platform we could just use DumbService.runWhenSmart, but in Rider
     * the C# language service needs also to wait for the solution to fully load and DumbService.runWhenSmart
     * does not guarantee that.
     * this method will add the task to every registered language service, usually there is only one,
     * but there could be two , for example if python plugin is installed on Rider there will be C# and
     * python language services, or if python plugin is installed on idea, so the task will run twice.
     * callers must take that into account.
     * This is made specifically for the initialization of SummaryViewService that needs language services
     * to be fully functioning on startup, in C# it means that the solution is fully loaded. actually supporting
     * C# on Rider is the main reason for this method because only the C# language service has access to the solution
     * and can test if it's fully loaded.
     */
    static void runWhenSmartForAll(@NotNull Project project,@NotNull Runnable task){
        Log.log(LOGGER::debug,"runWhenSmartForAll invoked");
        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                @SuppressWarnings("unchecked")
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                if (languageService != null) {
                    Log.log(LOGGER::debug,"calling runWhenSmart for {}",languageService);
                    languageService.runWhenSmart(task);
                }
            } catch (Exception e) {
                Log.debugWithException(LOGGER,e,"exception in runWhenSmartForAll {}",e.getMessage());
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //JavaLanguageService will fail to load on rider, etc.
                //This method is npt called a lot, only on startup.
            }
        }
    }

    void runWhenSmart(Runnable task);


    /**
     * if we have MethodInfo it should be easy to find the language. it should always be the preferred way when there
     * is a MethodInfo.
     */
    @NotNull
    static LanguageService findLanguageServiceByMethodInfo(@NotNull Project project, @NotNull MethodInfo methodInfo) {

        try {
            PsiFile psiFile = PsiUtils.uriToPsiFile(methodInfo.getContainingFileUri(), project);
            if (psiFile.isValid()) {
                Language language = psiFile.getLanguage();
                return project.getService(LanguageServiceLocator.class).locate(language);
            }
        } catch (PsiFileNotFountException e) {
            //ignore
        }

        //if not found above try another way
        return findLanguageServiceByMethodCodeObjectId(project, methodInfo.getId());

    }

    /**
     * This method is used to find the LanguageService to use in situations where we don't have a context like
     * MethodInfo or a file.
     * for example: ErrorsProvider.getErrorDetails may be clicked from the summary view, in that case there was no
     * code object discovery and the file is probably not opened. in that case we have only methodCodeObjectId.
     * Also, WorkspaceUrisHelper#findWorkspaceUrisForSpans calls this method if summary view has SpanDurationChangeInsight
     * and there is no context like MethodInfo or file.
     * This method must be executed in ReadAction or EDT.
     */
    @NotNull
    static LanguageService findLanguageServiceByMethodCodeObjectId(@NotNull Project project, @Nullable String methodCodeObjectId) {

        //first try to find a methodInfo, it will exist in documentInfoService if the document is opened in the editor.
        //it's the easiest way because documentInfoService has the information of which language this MethodInfo is.
        //if the document is not opened MethodInfo will be null.
        //if getErrorDetails is called from error insight then the document is opened for sure and MethodInfo will be found.
        MethodInfo methodInfo = null;
        if (methodCodeObjectId != null) {
            methodInfo = DocumentInfoService.getInstance(project).findMethodInfo(methodCodeObjectId);
        }

        //methodInfo may or may not exist already in documentInfoService, if exists find the language by methodInfo.
        Language language = null;
        if (methodInfo == null) {
            //first try to find language with LanguageService.findLanguageByMethodCodeObjectId which usually queries
            //intellij index
            if (methodCodeObjectId != null) {
                language = LanguageService.findLanguageByMethodCodeObjectId(project, methodCodeObjectId);
            }

            //last resort. dominant language will be available if at least one document was already opened.
            //dominant language will be null if no document was opened yet. it may happen if no document was opened and
            //user opens an error from the summary view.
            if (language == null) {
                language = DocumentInfoService.getInstance(project).getDominantLanguage();
            }
        } else {
            language = project.getService(DocumentInfoService.class).getLanguageByMethodCodeObjectId(methodInfo.getId());
        }

        if (language == null) {
            return NoOpLanguageService.INSTANCE;
        }

        return project.getService(LanguageServiceLocator.class).locate(language);
    }


    /**
     * This method should be used as last resort to find the language
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static Language findLanguageByMethodCodeObjectId(@NotNull Project project, @NotNull String methodId) {

        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                Language language = languageService.getLanguageForMethodCodeObjectId(methodId);
                if (language != null) {
                    return language;
                }
            } catch (Throwable e) {
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //JavaLanguageService will fail to load on rider, etc.
                //don't log, it will happen too many times
            }
        }

        return null;

    }


    /**
     * This method should be a last resort to find language as it is slow and not reliable.
     * try to find the language by method code object id.
     * each language service should implement it differently and may return null.
     * each language service can only check if this method's language is the language it supports.
     * So actually the language service can only return its supported language or null.
     */
    @Nullable
    Language getLanguageForMethodCodeObjectId(@NotNull String methodId);


    @SuppressWarnings("unused")
    boolean isSupportedFile(Project project, VirtualFile newFile);

    boolean isSupportedFile(Project project, PsiFile psiFile);

    //some language services need the editor, for example CSharpLanguageService needs to take
    // getProjectModelId from the selected editor which is the preferred way to find a IPsiSourceFile in resharper. it may be null.
    @NotNull
    MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile,@Nullable Editor selectedEditor, int caretOffset);


    /**
     * This method is called from the function list preview tab panel and is meant to navigate
     * to a method of the current opened file. it will not navigate to any method in the project.
     *
     * @param methodId the method id to navigate to
     */
    void navigateToMethod(String methodId);

    boolean isServiceFor(@NotNull Language language);

    Map<String, String> findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(List<String> methodCodeObjectIds);

    Map<String, Pair<String, Integer>> findWorkspaceUrisForMethodCodeObjectIds(List<String> methodCodeObjectIds);

    Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds);

    void environmentChanged(String newEnv);


    @NotNull
    DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile);

    //some language services need the selected editor , for example CSharpLanguageService need to take
    // getProjectModelId from the selected editor. it may be null
    @NotNull
    DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor selectedTextEditor);


    boolean isRelevant(VirtualFile file);

    boolean isRelevant(PsiFile psiFile);

    void refreshMethodUnderCaret(@NotNull Project project,@NotNull  PsiFile psiFile, @Nullable Editor selectedEditor, int offset);

}
