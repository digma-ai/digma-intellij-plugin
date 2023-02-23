package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.log.Log;
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

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle(ToolWindowUtil.TAB_NAME);
        ToolWindowFactory.super.init(toolWindow);
    }

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
        var contentFactory = ContentFactory.getInstance();

        var toolWindowTabsHelper = project.getService(ToolWindowTabsHelper.class);
        toolWindowTabsHelper.setToolWindow(toolWindow);

        //initialize AnalyticsService early so the UI already can detect the connection status when created
        project.getService(AnalyticsService.class);


        Content contentToSelect = createInsightsTab(project, toolWindow, contentFactory, toolWindowTabsHelper);
        createErrorsTab(project, toolWindow, contentFactory, toolWindowTabsHelper);
        createSummaryTab(project, toolWindow, contentFactory);

        ErrorsActionsService errorsActionsService = project.getService(ErrorsActionsService.class);
        toolWindow.getContentManager().addContentManagerListener(errorsActionsService);


        project.getService(ToolWindowShower.class).setToolWindow(toolWindow);

        toolWindow.getContentManager().setSelectedContent(contentToSelect, true);


        new Task.Backgroundable(project, "Digma: update views") {
            //sometimes the views models are updated before the tool window is initialized.
            //it happens when files are re-opened early before the tool window, and CaretContextService.contextChanged
            //is invoked and updates the models.
            //SummaryViewService is also initialized before the tool window is opened, it will get the event when
            // the environment is loaded and will update its model but will not update the ui because the panel is
            // not initialized yet.
            //only at this stage the panels are constructed already. just calling updateUi() for all view services
            // will actually update the UI.
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                project.getService(InsightsViewService.class).updateUi();
                project.getService(ErrorsViewService.class).updateUi();
                project.getService(SummaryViewService.class).updateUi();
            }
        }.queue();


        //sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it
        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
        if (backendConnectionMonitor.isConnectionOk()) {
            Backgroundable.ensureBackground(project, "change environment", () -> {
                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
                publisher.environmentChanged(project.getService(AnalyticsService.class).getEnvironment().getCurrent());
            });
        }

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

    private static void createErrorsTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory, ToolWindowTabsHelper toolWindowTabsHelper) {
        var errorsPanel = ErrorsTabKt.errorsPanel(project);
        var errorsViewService = project.getService(ErrorsViewService.class);
        errorsViewService.setPanel(errorsPanel);
        var errorsContent = contentFactory.createContent(errorsPanel, "Errors", false);
        errorsContent.setTabName(ToolWindowTabsHelper.ERRORS_TAB_NAME); //we use tab name as a key , changing the name will break the plugin
        errorsContent.setPreferredFocusedComponent(errorsPanel::getPreferredFocusedComponent);
        errorsContent.setPreferredFocusableComponent(errorsPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(errorsContent);
        errorsViewService.setContent(toolWindow, errorsContent);
        toolWindowTabsHelper.setErrorsContent(errorsContent);
    }


    @NotNull
    private static Content createInsightsTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory, ToolWindowTabsHelper toolWindowTabsHelper) {
        var insightsPanel = InsightsTabKt.insightsPanel(project);
        var insightsViewService = project.getService(InsightsViewService.class);
        insightsViewService.setPanel(insightsPanel);
        var insightsContent = contentFactory.createContent(insightsPanel, "Insights", false);
        insightsContent.setTabName(ToolWindowTabsHelper.INSIGHTS_TAB_NAME);//we use tab name as a key , changing the name will break the plugin
        insightsContent.setPreferredFocusedComponent(insightsPanel::getPreferredFocusedComponent);
        insightsContent.setPreferredFocusableComponent(insightsPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(insightsContent);
        insightsViewService.setContent(toolWindow, insightsContent);
        toolWindowTabsHelper.setInsightsContent(insightsContent);
        return insightsContent;
    }
}
