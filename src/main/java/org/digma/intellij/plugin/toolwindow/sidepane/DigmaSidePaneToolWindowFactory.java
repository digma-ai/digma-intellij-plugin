package org.digma.intellij.plugin.toolwindow.sidepane;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.common.IDEUtilsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceData;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.service.ErrorsActionsService;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.digma.intellij.plugin.ui.common.ContentPanel;
import org.digma.intellij.plugin.ui.common.statuspanels.NoConnectionPanelKt;
import org.digma.intellij.plugin.ui.panels.DisposablePanel;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.digma.intellij.plugin.ui.service.SummaryViewService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

import static org.digma.intellij.plugin.ui.common.InstallationWizardSidePanelWindowPanelKt.createInstallationWizardSidePanelWindowPanel;
import static org.digma.intellij.plugin.ui.common.MainSidePaneWindowPanelKt.createMainSidePaneWindowPanel;


/**
 * The main Digma tool window on left panel
 */
public class DigmaSidePaneToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaSidePaneToolWindowFactory.class);
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

        //initialize AnalyticsService early so the UI can detect the connection status when created
        AnalyticsService.getInstance(project);

        ToolWindowShower.getInstance(project).setToolWindow(toolWindow);

        var contentPanel = new ContentPanel(project);
        var mainSidePaneWindowPanel = createMainSidePaneWindowPanel(project,contentPanel);
        var cardsPanel = createCardsPanel(project,mainSidePaneWindowPanel);
        var mainContent =  ContentFactory.getInstance().createContent(cardsPanel, null, false);
        toolWindow.getContentManager().addContent(mainContent);


        //the mainContent is added by default to the tool window. it will be replaced if we need to show
        // the wizard.
        //wizardPanelBuilder is instead of moving some classes to ide-common. ideally we want things to be accessible in
        // ide-common. We want to build the wizard in MainToolWindowCardsController every time its necessary but the
        // method createInstallationWizardSidePanelWindowPanel is not accessible in ide-common module, to move it its
        // necessary to move more code. but anyway the Supplier is fine here.
        //when ever we need to show the wizard it will be created new and disposed when finished, its probably not a
        // good idea to keep it in memory after its finished.
        Supplier<DisposablePanel> wizardPanelBuilder = () -> createInstallationWizardSidePanelWindowPanel(project);
        MainToolWindowCardsController.getInstance(project).initComponents(toolWindow,mainContent,cardsPanel,contentPanel,wizardPanelBuilder);

        ErrorsActionsService errorsActionsService = project.getService(ErrorsActionsService.class);
        toolWindow.getContentManager().addContentManagerListener(errorsActionsService);

        PersistenceData persistenceDataState = PersistenceService.getInstance().getState();

        if (IDEUtilsService.isIdeaIDE() && persistenceDataState.getAlreadyPassedTheInstallationWizardForIdeaIDE() ||
                IDEUtilsService.isRiderIDE() && persistenceDataState.getAlreadyPassedTheInstallationWizardForRiderIDE() ||
                IDEUtilsService.isPyCharmIDE() && persistenceDataState.getAlreadyPassedTheInstallationWizardForPyCharmIDE()
        ) {
            //MainToolWindowCardsController.getInstance(project).showMainPanel();
        }else{
            MainToolWindowCardsController.getInstance(project).showWizard();
        }

        //todo: runWhenSmart is ok for java,python , but in Rider runWhenSmart does not guarantee that the solution
        // is fully loaded. consider replacing that with LanguageService.runWhenSmartForAll so that C# language service
        // can run this task when the solution is fully loaded.
        DumbService.getInstance(project).runWhenSmart(() -> initializeWhenSmart(project));
    }

    private JPanel createCardsPanel(@NotNull Project project, @NotNull JPanel mainPanel) {

        var cardLayout = new CardLayout();
        var cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(JBUI.Borders.empty());

        var noConnectionPanel = NoConnectionPanelKt.createNoConnectionPanel(project);
//        var nonSupportedPanel = createNonSupportedEmptyStatePanel(project);
//        var noFilePanel = createNoFileEmptyStatePanel(project);


        cardsPanel.add(mainPanel, MainToolWindowCardsController.MainWindowCard.MAIN.name());
        cardLayout.addLayoutComponent(mainPanel, MainToolWindowCardsController.MainWindowCard.MAIN.name());

        cardsPanel.add(noConnectionPanel, MainToolWindowCardsController.MainWindowCard.NO_CONNECTION.name());
        cardLayout.addLayoutComponent(noConnectionPanel, MainToolWindowCardsController.MainWindowCard.NO_CONNECTION.name());

//        cardsPanel.add(nonSupportedPanel, MainToolWindowCardsController.MainWindowCard.NON_SUPPORTED.name());
//        cardLayout.addLayoutComponent(nonSupportedPanel, MainToolWindowCardsController.MainWindowCard.NON_SUPPORTED.name());

//        cardsPanel.add(noFilePanel, "STARTUP");
//        cardLayout.addLayoutComponent(noFilePanel, "STARTUP");

        cardLayout.show(cardsPanel, MainToolWindowCardsController.MainWindowCard.MAIN.name());

        return cardsPanel;
    }


    private void initializeWhenSmart(@NotNull Project project) {

        Log.log(LOGGER::debug, "in initializeWhenSmart, dumb mode is {}", DumbService.isDumb(project));

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
}
