package org.digma.intellij.plugin.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.ElementUnderCaret;
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
    public void contextChanged(MethodUnderCaret elementUnderCaret) {

        Log.log(LOGGER::info, "contextChanged: {}",elementUnderCaret);
        insightsViewService.contextChanged(elementUnderCaret);
        //todo: errors
    }

    @Override
    public void contextEmpty() {
        //todo: implement
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
