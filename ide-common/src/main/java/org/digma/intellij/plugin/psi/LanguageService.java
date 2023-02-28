package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.document.DocumentInfoService;
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
            methodInfo = project.getService(DocumentInfoService.class).findMethodInfo(methodCodeObjectId);
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
                language = project.getService(DocumentInfoService.class).getDominantLanguage();
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


    boolean isSupportedFile(Project project, VirtualFile newFile);

    boolean isSupportedFile(Project project, PsiFile psiFile);

    @NotNull
    MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, int caretOffset);


    /**
     * This method is called from the function list preview tab panel and is meant to navigate
     * to a method of the current opened file. it will not navigate to any method in the project.
     *
     * @param codeObjectId the method id to navigate to
     */
    void navigateToMethod(String codeObjectId);

    boolean isServiceFor(@NotNull Language language);

    Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds);

    Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds);

    void environmentChanged(String newEnv);


    @NotNull
    DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile);

    /**
     * This method is meant to distinguish languages that are implemented as intellij platform plugin.
     * the main reason is that we need to know if this is not C# on rider. its used mainly to help in decisions about
     * how to process swing editor events.
     * for example, C# is not relevant for processing editor caret events or file open events because its implemented
     * in resharper and the plugin is notified about these events from resharper.
     *
     * @return true if this is an intellij platform event.
     */
    boolean isIntellijPlatformPluginLanguage();


    boolean isRelevant(VirtualFile file);

    boolean isRelevant(PsiFile psiFile);
}
