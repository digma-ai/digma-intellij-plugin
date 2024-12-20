package org.digma.intellij.plugin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.*;
import kotlin.jvm.functions.Function0;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower;
import org.digma.intellij.plugin.ui.panels.DisposablePanel;
import org.digma.intellij.plugin.updates.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

import static org.digma.intellij.plugin.analytics.EnvUtilsKt.refreshEnvironmentsNowOnBackground;
import static org.digma.intellij.plugin.scheduling.SchedulersKt.oneShotTask;

/**
 * Controls the current view in digma tool window.
 * there is a main content, wizard,troubleshooting and no connection.
 * the main content has a no connection empty card and the main card.
 * the tool window should have only one content.
 * Not handling any exceptions in this class. if an exception is thrown we must know that because it's probably a serious bug.
 */
public class MainToolWindowCardsController implements Disposable {


    private static final Logger LOGGER = Logger.getInstance(MainToolWindowCardsController.class);


    public enum MainWindowCard {
        MAIN, NO_CONNECTION, UPDATE_MODE
    }


    private final Project project;

    private ToolWindow toolWindow;

    //the main too window content, its replaced when opening the wizard
    private Content mainContent = null;

    //the main card panel, our main view and no-connection panel
    private JPanel cardsPanel;

    //never use latestCalledCard , only in initComponents
    private MainWindowCard latestCalledCard;

    //wizard content is created and disposed when necessary. WizardComponents keeps the reference to the content and the panel.
    private final WizardComponents wizard = new WizardComponents();
    private Function<Boolean, DisposablePanel> wizardPanelBuilder;

    private final TroubleshootingComponents troubleshooting = new TroubleshootingComponents();
    private Supplier<DisposablePanel> troubleshootingPanelBuilder;

    private final AtomicBoolean isConnectionLost = new AtomicBoolean(false);


    public MainToolWindowCardsController(@NotNull Project project) {
        this.project = project;

        project.getMessageBus()
                .connect()
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


        project.getMessageBus().connect().subscribe(AggressiveUpdateStateChangedEvent.Companion.getUPDATE_STATE_CHANGED_TOPIC(),
                (AggressiveUpdateStateChangedEvent) this::updateStateChanged);


        project.getMessageBus().connect().subscribe(ApiClientChangedEvent.getAPI_CLIENT_CHANGED_TOPIC(), (ApiClientChangedEvent) newUrl -> oneShotTask("MainToolWindowCardsController.apiClientChanged", (Function0<Void>) () -> {
            if (wizard.isOn()) {
                    //do here everything that happens on INSTALLATION_WIZARD/FINISH message
                    PersistenceService.getInstance().firstWizardLaunchDone();
                    EDT.ensureEDT(() -> {
                        ToolWindowShower.getInstance(project).showToolWindow();
                        RecentActivityToolWindowShower.getInstance(project).showToolWindow();
                        wizardFinished();
                    });
            }
            return null;
        }));
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
                               @NotNull Function<Boolean, DisposablePanel> wizardPanelBuilder,
                               Supplier<DisposablePanel> troubleshootingPanelBuilder) {

        Log.log(LOGGER::debug, "initComponents called");

        Log.log(LOGGER::debug, "got tool window {}", toolWindow.getId());
        this.toolWindow = toolWindow;

        Log.log(LOGGER::debug, "got mainContent {}", mainContent);
        this.mainContent = mainContent;
        Log.log(LOGGER::debug, "got cardPanel {}", mainCardsPanel);
        this.cardsPanel = mainCardsPanel;

        this.wizardPanelBuilder = wizardPanelBuilder;

        this.troubleshootingPanelBuilder = troubleshootingPanelBuilder;

        //it may be that there was a connection lost event before the panels were ready.
        // in that case show connection lost panel
        if (isConnectionLost.get() || BackendConnectionMonitor.getInstance(project).isConnectionError()) {
            showNoConnection();
        }

        //it may be that some showXXX is called before the components are initialized and
        // cardPanel was still null. one example is showUpdateBackendPanel that may be called very early.
        // so keep the latest called card and show it here after all components are initialized.
        if (latestCalledCard != null) {
            showCard(latestCalledCard);
        }
    }


    public void updateStateChanged(@NotNull PublicUpdateState updateState) {
        if (updateState.getUpdateState() == CurrentUpdateState.OK) {
            closeAggressiveUpdatePanel();
        } else {
            showAggressiveUpdatePanel();
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
            refreshEnvironmentsNowOnBackground(project);

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
            //dispose the troubleshooting panel which will dispose the jcef browser
            troubleshooting.troubleshootingPanel.dispose();
            troubleshooting.troubleshootingContent = null;
            troubleshooting.troubleshootingPanel = null;

            //refresh after troubleshooting finished will refresh environments and insights view
            refreshEnvironmentsNowOnBackground(project);

        } else {
            Log.log(LOGGER::debug, project, "troubleshootingFinished was called but troubleshooting is not on.");
        }
    }


    /**
     * it may happen that user clicks a span link while the troubleshooting or wizard are on.
     * it can happen if user opens troubleshooting or the wizard and then clicks a link in recent activity,
     * or in jaeger ui, or clicks a code lens. in all these cases we need to close the troubleshooting or wizard
     * and show main panel.
     */
    public void closeCoveringViewsIfNecessary() {

        //in case wizard is on and there is no connection don't change it. probably user is installing local
        // engine, or it's first launch of the wizard and is auto installing.
        if (wizard.isOn() && BackendConnectionMonitor.getInstance(project).isConnectionError()) {
            return;
        }

        //in case user finished new install but never clicked finish in the wizard, and clicks a link somewhere,
        // for example in recent activity, then close everything to show the main panel
        if (wizard.isOn()) {
            PersistenceService.getInstance().firstWizardLaunchDone();
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

            EDT.ensureEDT(() -> showCard(MainWindowCard.MAIN));
        }
    }

    private void showAggressiveUpdatePanel() {

        Log.log(LOGGER::debug, "showAggressiveUpdatePanel called");

        //replace the card even if wizard is on. it will not show until wizard content is removed.

        //this may happen on startup,showMainPanel is called from the tool window factory,
        // but there may be a connection lost before the content was built and before this controller was initialized
        if (isConnectionLost.get()) {
            Log.log(LOGGER::debug, "Not showing AggressiveUpdatePanel because connection lost, showing NoConnection");
            showNoConnection();
        } else {
            EDT.ensureEDT(() -> showCard(MainWindowCard.UPDATE_MODE));
        }
    }

    private void closeAggressiveUpdatePanel() {

        Log.log(LOGGER::debug, "closeAggressiveUpdatePanel called");


        //replace the card even if wizard is on. it will not show until wizard content is removed.

        //this may happen on startup,showMainPanel is called from the tool window factory,
        // but there may be a connection lost before the content was built and before this controller was initialized
        if (isConnectionLost.get()) {
            Log.log(LOGGER::debug, "Not showing MainPanel because connection lost, showing NoConnection");
            showNoConnection();
        } else {
            EDT.ensureEDT(() -> showCard(MainWindowCard.MAIN));
        }
    }


    private void showNoConnection() {
        Log.log(LOGGER::debug, "showNoConnection called");

        //replace the card even if wizard is on. it will not show until wizard content is removed.

        showCard(MainWindowCard.NO_CONNECTION);
    }


    private void showCard(MainWindowCard card) {
        Log.log(LOGGER::debug, "showCard called with {}", card);

        //need to keep the UPDATE_MODE if AggressiveUpdateService is still in update mode.
        // after AggressiveUpdateService enters update mode there may be connection lost, the connectionLost
        // will change to NO_CONNECTION, in that case we want to see the no connection message.
        // on connectionGained the listener will try to change it to MAIN but if
        // AggressiveUpdateService is still in update mode we need to replace back to UPDATE_MODE
        MainWindowCard cardToUse;
        if (AggressiveUpdateService.getInstance(project).isInUpdateMode() && card == MainWindowCard.MAIN) {
            cardToUse = MainWindowCard.UPDATE_MODE;
        } else {
            cardToUse = card;
        }

        latestCalledCard = card;
        if (cardsPanel == null) {
            Log.log(LOGGER::debug, project, "show {} was called but cardsPanel is null", card);
        } else {
            Log.log(LOGGER::debug, project, "Showing card {}", card);
            EDT.ensureEDT(() -> ((CardLayout) cardsPanel.getLayout()).show(cardsPanel, cardToUse.name()));
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


}
