package org.digma.intellij.plugin.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.DocumentInfoContainer;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

/**
 * A service to implement the interactions between listeners and UI components.
 * All work should be done in the EDT.
 */
public class EditorInteractionService implements CaretContextService, Disposable {

    private static final Logger LOGGER = Logger.getInstance(EditorInteractionService.class);

    private Project project;

    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final DocumentInfoService documentInfoService;

    public EditorInteractionService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public static EditorInteractionService getInstance(Project project) {
        return project.getService(EditorInteractionService.class);
    }

    @Override
    public void contextChanged(MethodUnderCaret methodUnderCaret) {

        Log.log(LOGGER::info, "contextChanged: {}", methodUnderCaret);
        /*
        Assuming here that we must have a MethodInfo in DocumentInfoService that was populated in an earlier stage.
        in Rider for example there is a cache that is updated as soon as documents change, and a listener that
        notifies the frontend about changes in discovery objects when a document is opened or changed in the editor.
        the frontend then takes this info and saves it in a local Map cache.
        if the timing is right then usually we will have a MethodInfo available at this stage.
        todo: one race condition that may happen in Rider is if a document that had no methods now has a new method from
         coding or paste, maybe the MethodUnderCaret event will happen soon enough so there is still no MethodInfo
         in DocumentInfoService.
        for now ignoring the MethodUnderCaret if MethodInfo was not found.

         */


        if (methodUnderCaret.getId().isBlank()) {
            Log.log(LOGGER::info, "No id in methodUnderCaret {}. ", methodUnderCaret);
            //if no id then try to show a preview for the document
            if (!methodUnderCaret.getFileUri().isBlank()){
                Log.log(LOGGER::info, "Showing document preview for {}. ", methodUnderCaret);
                DocumentInfoContainer documentInfoContainer =  documentInfoService.getDocumentInfo(methodUnderCaret);
                if (documentInfoContainer == null){
                    Log.log(LOGGER::info, "Could not find document info for {}, Showing empty preview.", methodUnderCaret);
                }else{
                    Log.log(LOGGER::info, "Found document info for {}. document: {}", methodUnderCaret,documentInfoContainer.getPsiFile());

                }

                insightsViewService.showDocumentPreviewList(documentInfoContainer,methodUnderCaret.getFileUri());
            }else{
                contextEmpty();
            }
        }else{
            MethodInfo methodInfo = documentInfoService.getMethodInfo(methodUnderCaret);
            if (methodInfo == null) {
                Log.log(LOGGER::warn, "Could not find MethodInfo for MethodUnderCaret {}. ", methodUnderCaret);
                contextEmpty();
            } else {
                Log.log(LOGGER::info, "Context changed to MethodInfo {}. ", methodInfo);
                insightsViewService.contextChanged(methodInfo);
                //todo: errors panel
            }

        }

    }

    @Override
    public void contextEmpty() {
        Log.log(LOGGER::info, "contextEmpty called");
        insightsViewService.empty();
    }

    @Override
    public void dispose() {
        Log.log(LOGGER::debug, "disposing..");
    }


    public void start(@NotNull Project project) {
        Log.log(LOGGER::debug, "starting..");
        this.project = project;
        EditorEventsHandler editorEventsHandler = project.getService(EditorEventsHandler.class);
        LanguageService languageService = project.getService(LanguageService.class);
        editorEventsHandler.start(project, this, languageService);
    }

}
