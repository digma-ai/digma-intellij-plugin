package org.digma.intellij.plugin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent;
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.panels.DisposablePanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.digma.intellij.plugin.persistence.PersistenceUtilsKt.updateInstallationWizardFlag;

/**
 * Controls the current view in digma tool window.
 * there is a main content and wizard content, home view and insights view.
 * the main content has a no connection empty card and the main card.
 * the tool window should have only one content.
 * Not handling any exceptions in this class. if an exception is thrown we must know that because it's probably a serious bug.
 */
public class MainToolWindowCardsController implements Disposable {


    private static final Logger LOGGER = Logger.getInstance(MainToolWindowCardsController.class);

    public enum MainWindowCard {
        MAIN, NO_CONNECTION
    }

    public enum ContentCard {
        HOME, INSIGHTS
    }


    private final Project project;

    private ToolWindow toolWindow;

    //the main too window content, its replaced when opening the wizard
    private Content mainContent = null;

    //the main card panel, our main view and no-connection panel
    private JPanel cardsPanel;

    //the home and insights cards
    private JPanel contentPanel;

    //wizard content is created and disposed when necessary. WizardComponents keeps the reference to the content and the panel.
    private final WizardComponents wizard = new WizardComponents();
    private Function<Boolean, DisposablePanel> wizardPanelBuilder;

    private TroubleshootingComponents troubleshooting = new TroubleshootingComponents();
    private Supplier<DisposablePanel> troubleshootingPanelBuilder;

    private final NotificationsComponents notifications = new NotificationsComponents();
    private Supplier<DisposablePanel> allNotificationsPanelBuilder;

    private final AtomicBoolean isConnectionLost = new AtomicBoolean(false);


    public MainToolWindowCardsController(@NotNull Project project) {
        this.project = project;

        project.getMessageBus()
                .connect(this)
                .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, new AnalyticsServiceConnectionEvent() {
                    @Override
                    public void connectionLost() {
                        Log.log(LOGGER::debug, "Got connectionLost");
                        isConnectionLost.set(true);
                        showNoConnection();
                    }

                    @Override
                    public void connectionGained() {
                        Log.log(LOGGER::debug, "Got connectionGained");
                        isConnectionLost.set(false);
                        showMainPanel();
                    }
                });

    }


    public static MainToolWindowCardsController getInstance(@NotNull Project project) {
        return project.getService(MainToolWindowCardsController.class);
    }


    @Override
    public void dispose() {
        //dispose if necessary
    }


    //this method must be called with valid non-null components on startup.
    // if any of these is null this controller will fail.
    // can't send those to the constructor because it's a plugin service.
    public void initComponents(@NotNull ToolWindow toolWindow,
                               @NotNull Content mainContent,
                               @NotNull JPanel mainCardsPanel,
                               @NotNull JPanel contentPanel,
                               @NotNull Function<Boolean, DisposablePanel> wizardPanelBuilder,
                               Supplier<DisposablePanel> troubleshootingPanelBuilder,
                               Supplier<DisposablePanel> allNotificationsPanelBuilder) {

        Log.log(LOGGER::debug, "initComponents called");

        Log.log(LOGGER::debug, "got tool window {}", toolWindow.getId());
        this.toolWindow = toolWindow;

        Log.log(LOGGER::debug, "got mainContent {}", mainContent);
        this.mainContent = mainContent;
        Log.log(LOGGER::debug, "got cardPanel {}", mainCardsPanel);
        this.cardsPanel = mainCardsPanel;

        Log.log(LOGGER::debug, "got contentPanel {}", contentPanel);
        this.contentPanel = contentPanel;

        this.wizardPanelBuilder = wizardPanelBuilder;

        this.troubleshootingPanelBuilder = troubleshootingPanelBuilder;

        this.allNotificationsPanelBuilder = allNotificationsPanelBuilder;

        //it may be that there was a connection lost event before the panels were ready.
        // in that case show connection lost panel
        if (isConnectionLost.get() || BackendConnectionMonitor.getInstance(project).isConnectionError()) {
            showNoConnection();
        }
    }


    public void showWizard(Boolean wizardSkipInstallationStep) {
        Log.log(LOGGER::debug, "showWizard called");

        //in case showWizard is called while wizard is already on
        if (wizard.isOn()) {
            Log.log(LOGGER::debug, project, "showWizard was called but wizardPanel on. nothing to do.");
            return;
        }

        //build the wizard panel every time its necessary and dispose it when the wizard finishes.
        var wizardPanel = wizardPanelBuilder.apply(wizardSkipInstallationStep);
        if (wizardPanel != null) {
            Content wizardContent = ContentFactory.getInstance().createContent(wizardPanel, null, false);
            toolWindow.getContentManager().removeContent(mainContent, false);
            toolWindow.getContentManager().addContent(wizardContent);
            wizard.wizardContent = wizardContent;
            wizard.wizardPanel = wizardPanel;
        } else {
            Log.log(LOGGER::debug, project, "showWizard was called but wizardPanel is null. it may happen if the runtime JVM does not support JCEF");
        }
    }


    //this is the only place to remove the wizard and add the main content.
    // it must be called when the wizard is finished or the wizard will stay on top.
    public void wizardFinished() {
        Log.log(LOGGER::debug, "wizardFinished called");

        if (wizard.isOn()) {
            toolWindow.getContentManager().removeContent(wizard.wizardContent, true);
            toolWindow.getContentManager().addContent(mainContent);
            //dispose the wizard panel which will dispose the jcef browser
            wizard.wizardPanel.dispose();
            wizard.wizardContent = null;
            wizard.wizardPanel = null;

            //refresh after wizard finished will refresh environments and insights view
            AnalyticsService.getInstance(project).getEnvironment().refreshNowOnBackground();

        } else {
            Log.log(LOGGER::debug, project, "wizardFinished was called but wizard is not on.");
        }
    }


    public void showTroubleshooting() {
        Log.log(LOGGER::debug, "showTroubleshooting called");

        //in case showWizard is called while wizard is already on
        if (troubleshooting.isOn() || wizard.isOn()) {
            Log.log(LOGGER::debug, project, "showTroubleshooting was called but troubleshooting on. nothing to do.");
            return;
        }

        //build the wizard panel every time its necessary and dispose it when the wizard finishes.
        var troubleshootingPanel = troubleshootingPanelBuilder.get();
        if (troubleshootingPanel != null) {

            Content troubleshootingContent = ContentFactory.getInstance().createContent(troubleshootingPanel, null, false);
//            if (wizard.isOn()) {
//                wizardFinished();
//            }

            toolWindow.getContentManager().removeContent(mainContent, false);
            toolWindow.getContentManager().addContent(troubleshootingContent);
            troubleshooting.troubleshootingContent = troubleshootingContent;
            troubleshooting.troubleshootingPanel = troubleshootingPanel;
        } else {
            Log.log(LOGGER::debug, project, "showTroubleshooting was called but troubleshootingPanel is null. it may happen if the runtime JVM does not support JCEF");
        }
    }


    //this is the only place to remove the wizard and add the main content.
    // it must be called when the wizard is finished or the wizard will stay on top.
    public void troubleshootingFinished() {
        Log.log(LOGGER::debug, "troubleshootingFinished called");

        if (troubleshooting.isOn()) {
            toolWindow.getContentManager().removeContent(troubleshooting.troubleshootingContent, true);
            toolWindow.getContentManager().addContent(mainContent);
            //dispose the wizard panel which will dispose the jcef browser
            troubleshooting.troubleshootingPanel.dispose();
            troubleshooting.troubleshootingContent = null;
            troubleshooting.troubleshootingPanel = null;

            //refresh after troubleshooting finished will refresh environments and insights view
            AnalyticsService.getInstance(project).getEnvironment().refreshNowOnBackground();

        } else {
            Log.log(LOGGER::debug, project, "troubleshootingFinished was called but troubleshooting is not on.");
        }
    }


    public void showAllNotifications() {
        Log.log(LOGGER::debug, "showAllNotifications called");

        //in case showAllNotifications is called while notifications is already on
        if (notifications.isOn()) {
            Log.log(LOGGER::debug, project, "showAllNotifications was called but notifications on. nothing to do.");
            return;
        }

        //build the notifications panel every time its necessary and dispose it when the notifications closed.
        var notificationsPanel = allNotificationsPanelBuilder.get();
        if (notificationsPanel != null) {
            Content notificationsContent = ContentFactory.getInstance().createContent(notificationsPanel, null, false);
            toolWindow.getContentManager().removeContent(mainContent, false);
            toolWindow.getContentManager().addContent(notificationsContent);
            notifications.notificationsContent = notificationsContent;
            notifications.notificationsPanel = notificationsPanel;
        } else {
            Log.log(LOGGER::debug, project, "showAllNotifications was called but notificationsPanel is null. it may happen if the runtime JVM does not support JCEF");
        }
    }


    public void closeAllNotificationsIfShowing() {
        if (notifications.isOn()) {
            closeAllNotifications();
        }
    }


    public void closeAllNotifications() {
        Log.log(LOGGER::debug, "closeAllNotifications called");

        EDT.ensureEDT(() -> {
            if (notifications.isOn()) {
                toolWindow.getContentManager().removeContent(notifications.notificationsContent, true);
                toolWindow.getContentManager().addContent(mainContent);
                //dispose the notifications panel which will dispose the jcef browser
                notifications.notificationsPanel.dispose();
                notifications.notificationsContent = null;
                notifications.notificationsPanel = null;

            } else {
                Log.log(LOGGER::debug, project, "closeAllNotifications was called but notifications is not on.");
            }
        });

    }




    /**
     * it may happen that user clicks a span link while the troubleshooting or wizard are on.
     * it can happen if user opens troubleshooting or the wizard and then clicks a link in recent activity,
     * or in jaeger ui, or clicks a code lens. in all these cases we need to close the troubleshooting or wizard
     * and show main panel.
     */
    public void closeCoveringViewsIfNecessary() {

        //Note in case user finished new install but never clicked finish in the wizard, and clicks a link somewhere,
        // then this method will close the wizard and update the wizard finished flags
        if (wizard.isOn()) {
            updateInstallationWizardFlag();
        }
        wizardFinished();
        troubleshootingFinished();
    }

    public void showMainPanel() {

        Log.log(LOGGER::debug, "showMainPanel called");

        //replace the card even if wizard is on. it will not show until wizard content is removed.

        //this may happen on startup,showMainPanel is called from the tool window factory,
        // but there may be a connection lost before the content was built and before this controller was initialized
        if (isConnectionLost.get()) {
            Log.log(LOGGER::debug, "Not showing MainPanel because connection lost, showing NoConnection");
            showNoConnection();
        } else {
            //FileEditorManager must be called on EDT
            EDT.ensureEDT(() -> showCard(MainWindowCard.MAIN));
        }
    }


    public void showHome() {
        showMainPanel();
        if (contentPanel == null) {
            Log.log(LOGGER::debug, project, "showHome was called but contentPanel is null");
        } else {
            Log.log(LOGGER::debug, project, "Showing home");
            EDT.ensureEDT(() -> ((CardLayout) contentPanel.getLayout()).show(contentPanel, ContentCard.HOME.name()));
        }
    }

    public void showInsights() {
        showMainPanel();
        if (contentPanel == null) {
            Log.log(LOGGER::debug, project, "showInsights was called but contentPanel is null");
        } else {
            Log.log(LOGGER::debug, project, "Showing insights");
            EDT.ensureEDT(() -> ((CardLayout) contentPanel.getLayout()).show(contentPanel, ContentCard.INSIGHTS.name()));
        }
    }


    private void showNoConnection() {
        Log.log(LOGGER::debug, "showNoConnection called");

        //replace the card even if wizard is on. it will not show until wizard content is removed.

        showCard(MainWindowCard.NO_CONNECTION);
    }


    private void showCard(MainWindowCard card) {
        Log.log(LOGGER::debug, "showCard called with {}", card);
        if (cardsPanel == null) {
            Log.log(LOGGER::debug, project, "show {} was called but cardsPanel is null", card);
        } else {
            Log.log(LOGGER::debug, project, "Showing card {}", card);
            EDT.ensureEDT(() -> ((CardLayout) cardsPanel.getLayout()).show(cardsPanel, card.name()));
        }
    }


    private static class WizardComponents {
        Content wizardContent;
        DisposablePanel wizardPanel;

        public boolean isOn() {
            return wizardContent != null && wizardPanel != null;
        }
    }

    private static class TroubleshootingComponents {
        Content troubleshootingContent;
        DisposablePanel troubleshootingPanel;

        public boolean isOn() {
            return troubleshootingContent != null && troubleshootingPanel != null;
        }
    }

    private static class NotificationsComponents {
        Content notificationsContent;
        DisposablePanel notificationsPanel;

        public boolean isOn() {
            return notificationsContent != null && notificationsPanel != null;
        }
    }

}
