package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.service.ErrorsActionsService;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.digma.intellij.plugin.ui.errors.ErrorsTabKt;
import org.digma.intellij.plugin.ui.insights.InsightsTabKt;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.digma.intellij.plugin.ui.service.SummaryViewService;
import org.digma.intellij.plugin.ui.service.ToolWindowTabsHelper;
import org.digma.intellij.plugin.ui.summary.SummaryTabKt;
import org.jetbrains.annotations.NotNull;


/**
 * The main Digma tool window on left panel
 */
public class DigmaLeftToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaLeftToolWindowFactory.class);
    private static final String DIGMA_NAME = "DIGMA";

    /**
     * this is the starting point of the plugin. this method is called when the tool window is opened.
     * before the window is opened there may be no reason to do anything, listen to events for example will be
     * a waste if the user didn't open the window. at least as much as possible, some extensions will be registered
     * but will do nothing if the plugin is not active.
     * after the plugin is active all listeners and extensions are installed and kicking until the IDE is closed.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);

        toolWindow.setTitle(DIGMA_NAME);
        //some language service should complete their startup on EDT,especially C# language service
        // needs to initialize its models on EDT.
        // startup may happen here if the tool window is opened on startup, or in EditorEventsHandler.selectionChanged
        // when the first document is opened.
        LanguageService.ensureStartupOnEDTForAll(project);

        var contentFactory = ContentFactory.getInstance();

        ToolWindowTabsHelper.getInstance(project).setToolWindow(toolWindow);

        //initialize AnalyticsService early so the UI can detect the connection status when created
        project.getService(AnalyticsService.class);


        Content contentToSelect = createInsightsTab(project, toolWindow, contentFactory);
        createErrorsTab(project, toolWindow, contentFactory);
        createSummaryTab(project, toolWindow, contentFactory);

        ErrorsActionsService errorsActionsService = project.getService(ErrorsActionsService.class);
        toolWindow.getContentManager().addContentManagerListener(errorsActionsService);


        ToolWindowShower.getInstance(project).setToolWindow(toolWindow);
        ToolWindowShower.getInstance(project).setInsightsTab(contentToSelect);


        toolWindow.getContentManager().setSelectedContent(contentToSelect, true);

        //todo: runWhenSmart is ok for java,python , but in Rider runWhenSmart does not guarantee that the solution
        // is fully loaded. consider replacing that with LanguageService.runWhenSmartForAll so that C# language service
        // can run this task when the solution is fully loaded.
        DumbService.getInstance(project).runWhenSmart(() -> initializeWhenSmart(project));

    }


    private void initializeWhenSmart(@NotNull Project project){

        Log.log(LOGGER::debug,"in initializeWhenSmart, dumb mode is {}", DumbService.isDumb(project));

        //sometimes the views models are updated before the tool window is initialized.
        //it happens when files are re-opened early before the tool window, and CaretContextService.contextChanged
        //is invoked and updates the models.
        //SummaryViewService is also initialized before the tool window is opened, it will get the event when
        // the environment is loaded and will update its model but will not update the ui because the panel is
        // not initialized yet.
        //only at this stage the panels are constructed already. just calling updateUi() for all view services
        // will actually update the UI.
        //todo: probably not necessary, EditorEventsHandler.selectionChanged loads DocumentInfo and
        // calls contextChanged only in smart mode. and in smart mode the panels should be constructed already.
        // needs some testing.
        // on the other hand if the tool window is opened after EditorEventsHandler.selectionChanged then the
        // models will be populated with data but updateUi was not invoked
        project.getService(InsightsViewService.class).updateUi();
        project.getService(ErrorsViewService.class).updateUi();
        project.getService(SummaryViewService.class).updateUi();


        //sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it.
        //todo: probably not necessary anymore because EditorEventsHandler.selectionChanged loads DocumentInfo and
        // calls contextChanged only in smart mode. so even when documents are opened in dumb mode the loading of
        // DocumentInfo, installing caret listener and change listener will occur in smart mode. so the situation
        // mentioned above should not happen.
        // on the other hand: in Rider, smart mode doesn't guarantee that the solution is fully loaded. so even if
        // EditorEventsHandler.selectionChanged loads DocumentInfo in smart mode it does not guarantee that C# language
        // service will have access to PSI references because the solution may still be loading. so calling that only
        // after the solution is fully loaded will guarantee full PSi access. see above, calling initializeWhenSmart
        // with LanguageService.runWhenSmartForAll will solve it.
//        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
//        if (backendConnectionMonitor.isConnectionOk()) {
//            Log.log(LOGGER::debug,"calling environmentChanged in background");
//            Backgroundable.ensureBackground(project, "change environment", () -> {
//                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
//                Log.log(LOGGER::debug,"calling environmentChanged with current environment to cause refresh of views in smart mode");
//                publisher.environmentChanged(project.getService(AnalyticsService.class).getEnvironment().getCurrent());
//            });
//        }
    }



    private static void createSummaryTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory) {
        var summaryPanel = SummaryTabKt.summaryPanel(project);
        var summaryViewService = project.getService(SummaryViewService.class);
        summaryViewService.setPanel(summaryPanel);
        var summaryContent = contentFactory.createContent(summaryPanel, "Summary", false);
        summaryContent.setTabName(ToolWindowTabsHelper.SUMMARY_TAB_NAME);
        summaryContent.setPreferredFocusedComponent(summaryPanel::getPreferredFocusedComponent);
        summaryContent.setPreferredFocusableComponent(summaryPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(summaryContent);
        summaryViewService.setContent(toolWindow, summaryContent);
    }

    private static void createErrorsTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory) {
        var errorsPanel = ErrorsTabKt.errorsPanel(project);
        var errorsViewService = project.getService(ErrorsViewService.class);
        errorsViewService.setPanel(errorsPanel);
        var errorsContent = contentFactory.createContent(errorsPanel, "Errors", false);
        errorsContent.setTabName(ToolWindowTabsHelper.ERRORS_TAB_NAME); //we use tab name as a key , changing the name will break the plugin
        errorsContent.setPreferredFocusedComponent(errorsPanel::getPreferredFocusedComponent);
        errorsContent.setPreferredFocusableComponent(errorsPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(errorsContent);
        errorsViewService.setContent(toolWindow, errorsContent);
        ToolWindowTabsHelper.getInstance(project).setErrorsContent(errorsContent);
    }


    @NotNull
    private static Content createInsightsTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory) {
        var insightsPanel = InsightsTabKt.insightsPanel(project);
        var insightsViewService = project.getService(InsightsViewService.class);
        insightsViewService.setPanel(insightsPanel);
        var insightsContent = contentFactory.createContent(insightsPanel, "Insights", false);
        insightsContent.setTabName(ToolWindowTabsHelper.INSIGHTS_TAB_NAME);//we use tab name as a key , changing the name will break the plugin
        insightsContent.setPreferredFocusedComponent(insightsPanel::getPreferredFocusedComponent);
        insightsContent.setPreferredFocusableComponent(insightsPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(insightsContent);
        insightsViewService.setContent(toolWindow, insightsContent);
        ToolWindowTabsHelper.getInstance(project).setInsightsContent(insightsContent);
        return insightsContent;
    }
}
