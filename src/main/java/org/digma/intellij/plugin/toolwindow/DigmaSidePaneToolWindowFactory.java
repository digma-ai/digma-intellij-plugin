package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.*;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.common.IDEUtilsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.troubleshooting.TroubleshootingPanel;
import org.digma.intellij.plugin.ui.*;
import org.digma.intellij.plugin.ui.common.MainContentPanel;
import org.digma.intellij.plugin.ui.common.statuspanels.NoConnectionPanelKt;
import org.digma.intellij.plugin.ui.notifications.AllNotificationsPanel;
import org.digma.intellij.plugin.ui.panels.DisposablePanel;
import org.digma.intellij.plugin.ui.service.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.*;

import static org.digma.intellij.plugin.ui.common.InstallationWizardSidePanelWindowPanelKt.createInstallationWizardSidePanelWindowPanel;
import static org.digma.intellij.plugin.ui.common.MainToolWindowPanelKt.createMainToolWindowPanel;


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


        //initialize AnalyticsService early so the UI can detect the connection status when created
        AnalyticsService.getInstance(project);

        if (!PersistenceService.getInstance().isFirstTimePluginLoaded()) {
            PersistenceService.getInstance().setFirstTimePluginLoaded();
            ActivityMonitor.getInstance(project).registerFirstTimePluginLoadedNew();
        }

        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);

        toolWindow.setTitle(DIGMA_NAME);
        //some language service should complete their startup on EDT,especially C# language service
        // needs to initialize its models on EDT.
        // startup may happen here if the tool window is opened on startup, or in EditorEventsHandler.selectionChanged
        // when the first document is opened.
        LanguageService.ensureStartupOnEDTForAll(project);

        ToolWindowShower.getInstance(project).setToolWindow(toolWindow);

        //contentPanel contains the main views, insights,assets,errors and tests
        var contentPanel = new MainContentPanel(project);
        //mainToolWindowPanel contains the navigation and update panels and the contentPanel
        var mainToolWindowPanel = createMainToolWindowPanel(project,contentPanel);
        //mainCardsPanel contains the mainToolWindowPanel and no connection panel
        var mainCardsPanel = createCardsPanel(project, mainToolWindowPanel, AnalyticsService.getInstance(project));
        //mainContent contains the mainCardsPanel ,MainToolWindowCardsController switches between no connection and mainToolWindowPanel
        var mainContent = ContentFactory.getInstance().createContent(mainCardsPanel, null, false);

        toolWindow.getContentManager().addContent(mainContent);


        //the mainContent is added by default to the tool window. it will be replaced if we need to show
        // the wizard.
        //wizardPanelBuilder is instead of moving some classes to ide-common. ideally we want things to be accessible in
        // ide-common. We want to build the wizard in MainToolWindowCardsController every time its necessary but the
        // method createInstallationWizardSidePanelWindowPanel is not accessible in ide-common module, to move it its
        // necessary to move more code. but anyway the Supplier is fine here.
        //when ever we need to show the wizard it will be created new and disposed when finished, its probably not a
        // good idea to keep it in memory after its finished.
        Function<Boolean, DisposablePanel> wizardPanelBuilder = wizardSkipInstallationStep -> createInstallationWizardSidePanelWindowPanel(project, wizardSkipInstallationStep);

        Supplier<DisposablePanel> troubleshootingPanelBuilder = () -> new TroubleshootingPanel(project);

        Supplier<DisposablePanel> allNotificationsPanelBuilder = () -> new AllNotificationsPanel(project);

        MainToolWindowCardsController.getInstance(project).initComponents(toolWindow, mainContent, mainCardsPanel,
                wizardPanelBuilder,
                troubleshootingPanelBuilder,
                allNotificationsPanelBuilder);

        if (IDEUtilsService.shouldOpenWizard()) {
            ActivityMonitor.getInstance(project).registerCustomEvent("show-installation-wizard", null);
            MainToolWindowCardsController.getInstance(project).showWizard(false);
        }
        else{
            ActivityMonitor.getInstance(project).registerCustomEvent("skip-installation-wizard", null);
        }


        //todo: runWhenSmart is ok for java,python , but in Rider runWhenSmart does not guarantee that the solution
        // is fully loaded. consider replacing that with LanguageService.runWhenSmartForAll so that C# language service
        // can run this task when the solution is fully loaded.
        DumbService.getInstance(project).runWhenSmart(() -> initializeWhenSmart(project));
    }

    private JPanel createCardsPanel(@NotNull Project project, @NotNull JPanel mainPanel, Disposable parentDisposable) {

        var cardLayout = new CardLayout();
        var cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(JBUI.Borders.empty());

        var noConnectionPanel = NoConnectionPanelKt.createNoConnectionPanel(project,parentDisposable);


        cardsPanel.add(mainPanel, MainToolWindowCardsController.MainWindowCard.MAIN.name());
        cardLayout.addLayoutComponent(mainPanel, MainToolWindowCardsController.MainWindowCard.MAIN.name());

        cardsPanel.add(noConnectionPanel, MainToolWindowCardsController.MainWindowCard.NO_CONNECTION.name());
        cardLayout.addLayoutComponent(noConnectionPanel, MainToolWindowCardsController.MainWindowCard.NO_CONNECTION.name());

        //start at home
        cardLayout.show(cardsPanel, MainToolWindowCardsController.MainWindowCard.MAIN.name());

        return cardsPanel;
    }


    private void initializeWhenSmart(@NotNull Project project) {

        Log.log(LOGGER::debug, "in initializeWhenSmart, dumb mode is {}", DumbService.isDumb(project));

        //sometimes the views models are updated before the tool window is initialized.
        //it happens when files are re-opened early before the tool window, and CaretContextService.contextChanged
        //is invoked and updates the models.
        //only at this stage the panels are constructed already. just calling updateUi() for all view services
        // will actually update the UI.
        //todo: probably not necessary, EditorEventsHandler.selectionChanged loads DocumentInfo and
        // calls contextChanged only in smart mode. and in smart mode the panels should be constructed already.
        // needs some testing.
        // on the other hand if the tool window is opened after EditorEventsHandler.selectionChanged then the
        // models will be populated with data but updateUi was not invoked
        project.getService(InsightsViewService.class).updateUi();
        project.getService(ErrorsViewService.class).updateUi();
    }
}
