package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import kotlin.Pair;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.env.Env;
import org.digma.intellij.plugin.instrumentation.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.jetbrains.annotations.*;

import java.util.*;

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
            } catch (Throwable e) {
                Log.debugWithException(LOGGER, e, "exception in ensureStartupOnEDTForAll {}", e.getMessage());
                //catch Throwable because there may be errors.
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
     */
    static void runWhenSmartForAll(@NotNull Project project, @NotNull Runnable task) {
        Log.log(LOGGER::debug, "runWhenSmartForAll invoked");
        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                @SuppressWarnings("unchecked")
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                if (languageService != null) {
                    Log.log(LOGGER::debug, "calling runWhenSmart for {}", languageService);
                    languageService.runWhenSmart(task);
                }
            } catch (Throwable e) {
                Log.debugWithException(LOGGER, e, "exception in runWhenSmartForAll {}", e.getMessage());
                //catch Throwable because there may be errors.
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //JavaLanguageService will fail to load on rider, etc.
                //This method is npt called a lot, only on startup.
            }
        }
    }

    void runWhenSmart(Runnable task);


    @NotNull
    static LanguageService findLanguageServiceByFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (ReadActions.isReadAccessAllowed()) {
            var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (PsiUtils.isValidPsiFile(psiFile)) {
                return LanguageServiceLocator.getInstance(project).locate(psiFile.getLanguage());
            } else {
                return findLanguageServiceByFileIfSupported(project, virtualFile);
            }
        } else {
            return findLanguageServiceByFileIfSupported(project, virtualFile);
        }
    }

    @NotNull
    static LanguageService findLanguageServiceByFileIfSupported(@NotNull Project project, @NotNull VirtualFile virtualFile) {

        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                var isSupported = languageService.isSupportedFile(project, virtualFile);
                if (isSupported) {
                    return languageService;
                }
            } catch (Throwable e) {
                //catch Throwable because there may be errors.
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //JavaLanguageService will fail to load on rider, etc.
                //don't log, it will happen too many times
            }
        }
        return NoOpLanguageService.INSTANCE;
    }



    /**
     * if we have MethodInfo it should be easy to find the language. it should always be the preferred way when there
     * is a MethodInfo.
     */
    @NotNull
    static LanguageService findLanguageServiceByMethodInfo(@NotNull Project project, @NotNull MethodInfo methodInfo) {

        try {
            PsiFile psiFile = PsiUtils.uriToPsiFile(methodInfo.getContainingFileUri(), project);
            if (PsiUtils.isValidPsiFile(psiFile)) {
                Language language = psiFile.getLanguage();
                return LanguageServiceLocator.getInstance(project).locate(language);
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
     * */
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
            //dominant language will be null if no document was opened yet.
            if (language == null) {
                language = DocumentInfoService.getInstance(project).getDominantLanguage();
            }
        } else {
            language = DocumentInfoService.getInstance(project).getLanguageByMethodCodeObjectId(methodInfo.getId());
        }

        if (language == null) {
            return NoOpLanguageService.INSTANCE;
        }

        return LanguageServiceLocator.getInstance(project).locate(language);
    }

    @NotNull
    static LanguageService findLanguageServiceByClassName(@NotNull Project project, @NotNull String className) {

        var language = LanguageService.findLanguageByClassName(project, className);
        if (language == null) {
            return NoOpLanguageService.INSTANCE;
        }

        return LanguageServiceLocator.getInstance(project).locate(language);
    }


    @Nullable
    static LanguageService findLanguageServiceByName(Project project, String languageServiceClassName) {
        try {
            return (LanguageService) project.getService(Class.forName(languageServiceClassName));
        } catch (Throwable e) {
            return null;
        }
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
                //catch Throwable because there may be errors.
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //JavaLanguageService will fail to load on rider, etc.
                //don't log, it will happen too many times
            }
        }

        return null;

    }

    @Nullable
    private static Language findLanguageByClassName(@NotNull Project project, @NotNull String className) {

        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                Language language = languageService.getLanguageForClass(className);
                if (language != null) {
                    return language;
                }
            } catch (Throwable e) {
                //catch Throwable because there may be errors.
                //ignore: some classes will fail to load , for example the CSharpLanguageService
                //will fail to load if it's not rider because it depends on rider classes.
                //JavaLanguageService will fail to load on rider, etc.
                //don't log, it will happen too many times
            }
        }

        return null;

    }


    @Nullable
    static Set<EndpointInfo> getEndpointInfos(Project project, String endpointCodeObjectId) {
        for (SupportedLanguages value : SupportedLanguages.values()) {

            try {
                Class<? extends LanguageService> clazz = (Class<? extends LanguageService>) Class.forName(value.getLanguageServiceClassName());
                LanguageService languageService = project.getService(clazz);
                var endpointInfos = languageService.lookForDiscoveredEndpoints(endpointCodeObjectId);
                if (endpointInfos != null && !endpointInfos.isEmpty()) {
                    return endpointInfos;
                }

            } catch (Throwable e) {
                //catch Throwable because there may be errors.
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

    @Nullable
    Language getLanguageForClass(@NotNull String className);


    @SuppressWarnings("unused")
    boolean isSupportedFile(Project project, VirtualFile newFile);

    boolean isSupportedFile(PsiFile psiFile);

    //some language services need the editor, for example CSharpLanguageService needs to take
    // getProjectModelId from the selected editor which is the preferred way to find a IPsiSourceFile in resharper. it may be null.
    @NotNull
    MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int caretOffset);

    @Nullable
    default String detectMethodBySpan(@NotNull Project project, String spanCodeObjectId) {
        return null;
    }

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

    Set<EndpointInfo> lookForDiscoveredEndpoints(String endpointId);

    /**
     * let language services do something on environmentChanged. for example to update the current method context.
     */
    void environmentChanged(Env newEnv);


    @NotNull
    DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, BuildDocumentInfoProcessContext context);

    //some language services need the selected editor , for example CSharpLanguageService need to take
    // getProjectModelId from the selected editor. it may be null
    @NotNull
    DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor selectedTextEditor, BuildDocumentInfoProcessContext context);


    boolean isRelevant(VirtualFile file);

    boolean isRelevant(PsiFile psiFile);

    void refreshMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int offset);

    boolean isCodeVisionSupported();

    @NotNull
    default MethodObservabilityInfo canInstrumentMethod(@NotNull String methodId) {
        return new MethodObservabilityInfo(methodId, false, false, null, false);
    }

    default boolean instrumentMethod(@NotNull MethodObservabilityInfo methodObservabilityInfo) {
        return false;
    }

    default void addDependencyToOtelLib(@NotNull String methodId) {
        //only relevant for jvm languages
        //todo: maybe throw non supported operation ?
    }

    @Nullable
    PsiElement getPsiElementForMethod(@NotNull String methodId);

    @Nullable
    PsiElement getPsiElementForClassByMethodId(@NotNull String methodId);

    @Nullable
    PsiElement getPsiElementForClassByName(@NotNull String className);

    @NotNull
    InstrumentationProvider getInstrumentationProvider();

    @NotNull
    Map<String, PsiElement> findMethodsByCodeObjectIds(@NotNull PsiFile psiFile, @NotNull List<String> methodIds) throws Throwable;
}