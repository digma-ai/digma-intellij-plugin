package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface LanguageService {

    Logger LOGGER = Logger.getInstance(LanguageService.class);


    /**
     * there are a few services that need to guess which language service to use. usually when there is no related
     * file.
     * <p>
     * examples are:
     * SlowestSpansHelper.findWorkspaceUrisForSpans: when this method is called while building SlowestSpansInsight
     * then we have the method id, and it will be found by DocumentInfoService because the related file is probably
     * opened in the editor and DocumentInfoService has the knowledge of the language.
     * but if it's called from SummariesProvider there is no method and we have to guess.
     * <p>
     * ErrorsProvider.getErrorDetails: if this method is called when the error is clicked in errors insight then we have
     * the method id and we can find the language because the document is probably opened and DocumentInfoService has the
     * knowledge of the language. but if called when clicking an error in summary view the document is probably not
     * opened and we have to guess.
     */
    @NotNull
    static LanguageService findLanguageServiceByMethodCodeObjectId(@NotNull Project project, @Nullable String methodCodeObjectId) {

        //first try to find a methodInfo, it will exist in documentInfoService if the document is opened in the editor.
        //it's the easiest way because from documentInfoService has the information of which language this MethodInfo is.
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

    @Nullable
    static Language findLanguageByMethodCodeObjectId(@NotNull Project project, @NotNull String methodId) {

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
     * try to find the language by method code object id.
     * each language service should implement it differently and may return null.
     * each language service can only check if this method's language is the language it supports.
     * So actually the language service can only return its supported language or null.
     */
    @Nullable
    Language getLanguageForMethodCodeObjectId(@NotNull String methodId);


    boolean isSupportedFile(Project project, VirtualFile newFile);

    boolean isSupportedFile(Project project, PsiFile psiFile);

    MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, int caretOffset);


    //for use by language services that have a psi impl, python, java, Not rider
    @Nullable
    default PsiElement findElementUnderCaret(@NotNull Project project, PsiFile psiFile, int caretOffset) {
        if (psiFile != null) {
            PsiElement psiElement = psiFile.findElementAt(caretOffset);
            Log.log(LOGGER::debug, "got psi element {}", psiElement);
            return psiElement;
        }
        return null;
    }


    void navigateToMethod(String codeObjectId);

    boolean isServiceFor(Language language);

    Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds);

    Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds);

    void environmentChanged(String newEnv);

    /**
     * this method has kind of the same purpose as isIntellijPlatformPluginLanguage , to distinguish languages that are
     * indexed in intellij FileBasedIndex
     *
     * @return true if this language in indexed in FileBasedIndex
     */
    boolean isIndexedLanguage();


    DocumentInfo buildDocumentInfo(PsiFile psiFile);

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
}
