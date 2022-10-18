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
import org.digma.intellij.plugin.service.EditorInteractionService;
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
 * The main Digma tool window
 */
public class DigmaToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaToolWindowFactory.class);

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

        var contentFactory = ContentFactory.SERVICE.getInstance();

        var toolWindowTabsHelper = project.getService(ToolWindowTabsHelper.class);
        toolWindowTabsHelper.setToolWindow(toolWindow);

        //initialize AnalyticsService early so the UI already can detect the connection status when created
        project.getService(AnalyticsService.class);

        Content contentToSelect;


        {
            var insightsPanel = InsightsTabKt.insightsPanel(project);
            var insightsViewService = project.getService(InsightsViewService.class);
            insightsViewService.setPanel(insightsPanel);
            var insightsContent = contentFactory.createContent(insightsPanel, "Insights", false);
            insightsContent.setTabName(ToolWindowTabsHelper.INSIGHTS_TAB_NAME);//we use tab name as a key , changing the name will break the plugin
            insightsContent.setPreferredFocusedComponent(insightsPanel::getPreferredFocusedComponent);
            insightsContent.setPreferredFocusableComponent(insightsPanel.getPreferredFocusableComponent());
            toolWindow.getContentManager().addContent(insightsContent);
            insightsViewService.setContent(toolWindow,insightsContent);
            toolWindowTabsHelper.setInsightsContent(insightsContent);
            contentToSelect = insightsContent;
        }

        {
            var errorsPanel = ErrorsTabKt.errorsPanel(project);
            var errorsViewService = project.getService(ErrorsViewService.class);
            errorsViewService.setPanel(errorsPanel);
            var errorsContent = contentFactory.createContent(errorsPanel, "Errors", false);
            errorsContent.setTabName(ToolWindowTabsHelper.ERRORS_TAB_NAME); //we use tab name as a key , changing the name will break the plugin
            errorsContent.setPreferredFocusedComponent(errorsPanel::getPreferredFocusedComponent);
            errorsContent.setPreferredFocusableComponent(errorsPanel.getPreferredFocusableComponent());
            toolWindow.getContentManager().addContent(errorsContent);
            errorsViewService.setContent(toolWindow,errorsContent);
            toolWindowTabsHelper.setErrorsContent(errorsContent);
        }

        {
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

        ErrorsActionsService errorsActionsService = project.getService(ErrorsActionsService.class);
        toolWindow.getContentManager().addContentManagerListener(errorsActionsService);


        project.getService(ToolWindowShower.class).setToolWindow(toolWindow);
        EditorInteractionService.getInstance(project).start();

        toolWindow.getContentManager().setSelectedContent(contentToSelect, true);

        new Task.Backgroundable(project, "Digma: Summary view reload...") {
            //SummaryViewService will get the event when the environment is loaded and will update its model,
            // but if the tool window is not opened yet then the summary tab will not update because the panel is not
            // available yet. only at this stage the panels are constructed already. just calling SummaryViewService.updateUi()
            // will do the job.
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                project.getService(SummaryViewService.class).updateUi();
            }
        }.queue();


        //todo: check and remove, find another solution
        //todo: sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it
        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
        if (backendConnectionMonitor.isConnectionOk()) {
            Backgroundable.ensureBackground(project, "change environment", () -> {
                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
                publisher.environmentChanged(project.getService(AnalyticsService.class).getEnvironment().getCurrent());
            });
        }

    }
}
