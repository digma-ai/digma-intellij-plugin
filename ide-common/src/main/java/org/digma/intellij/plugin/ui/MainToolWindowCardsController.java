package org.digma.intellij.plugin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class MainToolWindowCardsController implements Disposable {

    //todo: maybe the methods in this class should be synchronized

    private static final Logger LOGGER = Logger.getInstance(MainToolWindowCardsController.class);


    public enum MainWindowCard {
        MAIN, WIZARD, NO_CONNECTION, NON_SUPPORTED, EMPTY_EDITOR
    }


    @Override
    public void dispose() {
        //dispose if necessary
    }


    private final Project project;
    private JPanel cardsPanel;
    private boolean isWizardPanelExists = true;

    private boolean isWizardOn = false;
    private boolean isConnectionLost = false;

    //latestRequestedCard is used when connection gained to show the latest request if any.
    private MainWindowCard latestRequestedCard = null;


    public MainToolWindowCardsController(@NotNull Project project) {
        this.project = project;

        project.getMessageBus()
                .connect(this)
                .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, new AnalyticsServiceConnectionEvent() {
                    @Override
                    public void connectionLost() {
                        Log.log(LOGGER::debug,"Got connectionLost");
                        isConnectionLost = true;
                        showNoConnection();
                    }

                    @Override
                    public void connectionGained() {
                        Log.log(LOGGER::debug,"Got connectionGained");
                        isConnectionLost = false;
                        showLatestRequest();
                    }
                });

    }




    public static MainToolWindowCardsController getInstance(@NotNull Project project) {
        return project.getService(MainToolWindowCardsController.class);
    }


    public void setCardsPanel(JPanel cardsPanel,boolean isWizardPanelExists) {
        this.cardsPanel = cardsPanel;
        this.isWizardPanelExists = isWizardPanelExists;
        //it may be that there was a connection lost event before the panel was ready.
        // in that case show connection lost panel
        if (isConnectionLost){
            showNoConnection();
        }
    }


    public void showMainPanel() {
        Log.log(LOGGER::debug,"showMainPanel called");

        latestRequestedCard = MainWindowCard.MAIN;
        if (isWizardOn){
            Log.log(LOGGER::debug,"Not showing MAIN panel because wizard is on");
            return;
        }

        if (isConnectionLost){
            Log.log(LOGGER::debug,"Not showing MainPanel because connection lost, showing NoConnection");
            showNoConnection();
        }else{
            EDT.ensureEDT(() -> {
                if (noFilesOpen()){
                    Log.log(LOGGER::debug,"No files opened, calling showNoFile from showMainPanel");
                    showNoFile();
                }else{
                    showCard(MainWindowCard.MAIN);
                }
            });
        }
    }




    public void showWizard() {
        Log.log(LOGGER::debug,"showWizard called");
        EDT.ensureEDT(() -> {
            if (isWizardPanelExists) {
                isWizardOn = true;
                showCard(MainWindowCard.WIZARD);
            }else{
                Log.log(LOGGER::debug,project,"show WIZARD was called but wizard panel does not exists");
            }
        });
    }

    public void wizardClosedShowMainPanel() {
        Log.log(LOGGER::debug,"wizardClosedShowMainPanel called");
        isWizardOn = false;
        showLatestRequest();
        AnalyticsService.getInstance(project).getEnvironment().refreshNowOnBackground();
    }

    public void showNoConnection() {
        Log.log(LOGGER::debug,"showNoConnection called");
        if (isWizardOn){
            Log.log(LOGGER::debug,"Not showing NoConnection because wizard is on");
            return;
        }
        EDT.ensureEDT(() -> showCard(MainWindowCard.NO_CONNECTION));

    }

    public void showNonSupported() {
        Log.log(LOGGER::debug,"showNonSupported called");
        latestRequestedCard = MainWindowCard.NON_SUPPORTED;
        if (isWizardOn){
            Log.log(LOGGER::debug,"Not showing NonSupported panel because wizard is on");
            return;
        }
        if (isConnectionLost){
            Log.log(LOGGER::debug,"Not showing NonSupported because connection lost, showing NoConnection");
            showNoConnection();
        }else{
            EDT.ensureEDT(() -> showCard(MainWindowCard.NON_SUPPORTED));
        }


    }
    public void showNoFile() {
        Log.log(LOGGER::debug,"showNoFile called");
        latestRequestedCard = MainWindowCard.EMPTY_EDITOR;
        if (isWizardOn){
            Log.log(LOGGER::debug,"Not showing NoFile panel because wizard is on");
            return;
        }
        if (isConnectionLost){
            Log.log(LOGGER::debug,"Not showing NoFile because connection lost, showing NoConnection");
            showNoConnection();
        }else{
            EDT.ensureEDT(() -> showCard(MainWindowCard.EMPTY_EDITOR));
        }
    }


    private void showLatestRequest() {
        if (isWizardOn){
            return;
        }
        Log.log(LOGGER::debug,"showLatestRequest called");
        if (latestRequestedCard == null){
            Log.log(LOGGER::debug,"latestRequestedCard is null, showing MAIN");
            showMainPanel();
        }else{
            Log.log(LOGGER::debug,"showLatestRequest called, showing {}",latestRequestedCard);
            switch (latestRequestedCard){
                case NON_SUPPORTED -> showNonSupported();
                case EMPTY_EDITOR -> showNoFile();
                default -> showMainPanel();
            }
        }
    }


    private void showCard(MainWindowCard card){
        Log.log(LOGGER::debug,"showCard called with {}",card);
        if (cardsPanel == null){
            Log.log(LOGGER::debug,project,"show {} was called but cardsPanel is null",card);
        }else{
            ((CardLayout)cardsPanel.getLayout()).show(cardsPanel,card.name());
        }
    }


    private boolean noFilesOpen() {
        return FileEditorManager.getInstance(project).getOpenFiles().length == 0;
    }



    //for debugging only.
    //it's not easy to debug this class with debugger because debugger changes focus behaviour,
    // it's easier with debug logging and printStackTrace can help to understand who called this class
//    private void printStackTrace() {
//        Log.log(LOGGER::debug,"Stack trace:");
//        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
//            Log.log(LOGGER::debug,ste.toString());
//        }
//    }
}
