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

import java.util.ArrayList;

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

    public static CaretContextService getInstance(Project project) {
        return project.getService(CaretContextService.class);
    }

    @Override
    public void contextChanged(MethodUnderCaret methodUnderCaret) {

        Log.log(LOGGER::debug, "contextChanged: {}", methodUnderCaret);
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
            Log.log(LOGGER::debug, "No id in methodUnderCaret,trying fileUri {}. ", methodUnderCaret);
            //if no id then try to show a preview for the document
            if (methodUnderCaret.getFileUri().isBlank()){
                Log.log(LOGGER::debug, "No id and no fileUri in methodUnderCaret,clearing context {}. ", methodUnderCaret);
                contextEmpty();
            }else{
                Log.log(LOGGER::debug, "Showing document preview for {}. ", methodUnderCaret);
                DocumentInfoContainer documentInfoContainer =  documentInfoService.getDocumentInfo(methodUnderCaret);
                if (documentInfoContainer == null){
                    Log.log(LOGGER::debug, "Could not find document info for {}, Showing empty preview.", methodUnderCaret);
                }else{
                    Log.log(LOGGER::debug, "Found document info for {}. document: {}", methodUnderCaret,documentInfoContainer.getPsiFile());
                }

                insightsViewService.showDocumentPreviewList(documentInfoContainer,methodUnderCaret.getFileUri());
                errorsViewService.showDocumentPreviewList(documentInfoContainer,methodUnderCaret.getFileUri());
            }
        }else{
            MethodInfo methodInfo = documentInfoService.getMethodInfo(methodUnderCaret);
            if (methodInfo == null) {
                Log.log(LOGGER::warn, "Could not find MethodInfo for MethodUnderCaret {}. ", methodUnderCaret);
                //this happens when we don't have method info for a real method, usually when a class doesn't have
                //code objects found during discovery, it can be synthetic or auto-generated methods.
                //pass a dummy method info just to populate the view,the view is aware and will not try to query for insights.
                var dummyMethodInfo = new MethodInfo(methodUnderCaret.getId(), methodUnderCaret.getName(), methodUnderCaret.getClassName(), "",
                        methodUnderCaret.getFileUri(), 0, new ArrayList<>());
                Log.log(LOGGER::warn, "Using dummy MethodInfo for to update views {}. ", dummyMethodInfo);
                insightsViewService.contextChangeNoMethodInfo(dummyMethodInfo);
                errorsViewService.contextChangeNoMethodInfo(dummyMethodInfo);
            } else {
                Log.log(LOGGER::debug, "Context changed to {}. ", methodInfo);
                insightsViewService.contextChanged(methodInfo);
                errorsViewService.contextChanged(methodInfo);
            }

        }

    }

    @Override
    public void contextEmpty() {
        Log.log(LOGGER::debug, "contextEmpty called");
        insightsViewService.empty();
        errorsViewService.empty();
    }

    @Override
    public void dispose() {
        Log.log(LOGGER::debug, "disposing..");
    }


    public void start(@NotNull Project project) {
        Log.log(LOGGER::info, "starting..");
        this.project = project;
        EditorEventsHandler editorEventsHandler = project.getService(EditorEventsHandler.class);
        LanguageService languageService = project.getService(LanguageService.class);
        editorEventsHandler.start(project, this, languageService);
    }

}
