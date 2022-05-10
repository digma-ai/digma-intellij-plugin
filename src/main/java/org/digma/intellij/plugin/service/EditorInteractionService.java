package org.digma.intellij.plugin.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.MethodUnderCaret;
import org.digma.intellij.plugin.model.rest.MethodCodeObjectSummary;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.MethodContextUpdater;
import org.digma.intellij.plugin.ui.service.ErrorsService;
import org.digma.intellij.plugin.ui.service.InsightsService;
import org.jetbrains.annotations.NotNull;

/**
 * A service to implement the interactions between listeners and UI components.
 * All work should be done in the EDT.
 */
public class EditorInteractionService implements MethodContextUpdater, Disposable {

    private static final Logger LOGGER = Logger.getInstance(EditorInteractionService.class);

    private Project project;

    private final InsightsService insightsService;
    private final ErrorsService errorsService;
    private final DocumentInfoService documentInfoService;

    public EditorInteractionService(Project project) {
        this.project = project;
        insightsService = project.getService(InsightsService.class);
        errorsService = project.getService(ErrorsService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public static EditorInteractionService getInstance(Project project) {
        return project.getService(EditorInteractionService.class);
    }

    @Override
    public void updateViewContent(MethodUnderCaret methodUnderCaret) {

        MethodCodeObjectSummary methodCodeObjectSummary = documentInfoService.getMethodSummaries(methodUnderCaret);
        insightsService.updateSelectedMethod(methodUnderCaret,methodCodeObjectSummary);
        //todo: errors
    }

    @Override
    public void clearViewContent() {
        insightsService.empty();
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
