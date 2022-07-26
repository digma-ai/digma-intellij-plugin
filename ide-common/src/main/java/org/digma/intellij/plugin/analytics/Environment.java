package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.persistence.PersistenceData;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsListChangedListener;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Environment implements EnvironmentsSupplier {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);

    private String current;
    private final SettingsState settingsState;

    @NotNull
    private List<String> environments = new ArrayList<>();

    private final Project project;
    private final AnalyticsService analyticsService;
    private final PersistenceData persistenceData;

    private final Set<EnvironmentsListChangedListener> listeners = new LinkedHashSet<>();

    private Instant lastRefreshTimestamp = Instant.now();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService, @NotNull PersistenceData persistenceData, SettingsState settingsState) {
        this.project = project;
        this.analyticsService = analyticsService;
        this.persistenceData = persistenceData;
        this.current = persistenceData.getCurrentEnv();
        this.settingsState = settingsState;
    }



    @Override
    public String getCurrent() {
        //using getCurrent as a hook to recover environments in case it could not be loaded yet.
        //maybeRecoverEnvironments will run in the background. if current is null the method will return null,
        //if the environments where recovered the next call to getCurrent will return an environment.
        maybeRecoverEnvironments();
        return current;
    }

    @Override
    public void setCurrent(String newEnv) {

        Log.log(LOGGER::debug, "Setting current environment , old={},new={}", this.current, newEnv);

        //don't change or fire the event if it's the same env. it happens because we have two combobox, one on each tab
        if (Objects.equals(this.current, newEnv)) {
            return;
        }

        var oldEnv = this.current;
        this.current = newEnv;
        persistenceData.setCurrentEnv(newEnv);

        notifyEnvironmentChanged(oldEnv, newEnv);
    }



    @NotNull
    @Override
    public List<String> getEnvironments() {
        return environments;
    }


    @Override
    public void addEnvironmentsListChangeListener(EnvironmentsListChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            listeners.add(listener);
        }
    }



    @Override
    public void refresh() {

        if (!timeToRefresh()){
            Log.log(LOGGER::debug, "Skipping Refresh Environments , will try again in few seconds..");
            return;
        }

        new Task.Backgroundable(project, "Digma: Refreshing environments...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                refreshEnvironments();
            }
        }.queue();
    }



    private boolean timeToRefresh() {
        //don't try to refresh to often, It's usually not necessary
        var now = Instant.now();
        Duration duration = Duration.between(lastRefreshTimestamp, now);
        if (duration.getSeconds() < settingsState.refreshDelay){
            return false;
        }
        lastRefreshTimestamp = now;
        return true;
    }


    /**
     * maybeRecoverEnvironments is a hook to try and recover the environments list in case it could not be loaded yet.
     * it may be called in several events and will try to recover the environments list in the background.
     */
    private void maybeRecoverEnvironments() {
        if (environments == null || environments.isEmpty()) {
            Log.log(LOGGER::debug, "Environments list is empty,trying recovery");
            refresh();
        }
    }


    //this method should not be called on ui threads, it may hang and cause a freeze
    private void refreshEnvironments() {

        Log.log(LOGGER::debug, "Refresh Environments called");

        Log.log(LOGGER::debug, "Refreshing Environments list");
        var newEnvironments = analyticsService.getEnvironments();
        if (newEnvironments != null && newEnvironments.size() > 0) {
            Log.log(LOGGER::debug, "Got environments {}", newEnvironments);
        } else {
            Log.log(LOGGER::warn, "Error loading environments: {}", newEnvironments);
            newEnvironments = new ArrayList<>();
        }

        if (environmentsListEquals(newEnvironments, environments)) {
            return;
        }

        replaceEnvironmentsList(newEnvironments);
    }


    //this method may be called from both ui threads or background threads
    void replaceEnvironmentsList(@NotNull List<String> envs) {
        this.environments = envs;
        var oldEnv = current;
        if (current == null || !this.environments.contains(current)) {
            current = environments.size() > 0 ? environments.get(0) : null;
        }

        persistenceData.setCurrentEnv(current);
        fireEnvironmentsListChange();

        if (!Objects.equals(oldEnv, current)) {
            notifyEnvironmentChanged(oldEnv, current);
        }
    }


    private boolean environmentsListEquals(List<String> envs1, List<String> envs2) {
        if (envs1 == null && envs2 == null) {
            return true;
        }

        if (envs1 != null && envs2 != null && envs1.size() == envs2.size()) {
            return new HashSet<>(envs1).containsAll(envs2);
        }

        return false;
    }


    private void fireEnvironmentsListChange() {
        Log.log(LOGGER::debug, "Firing environmentsListChanged event");
        Runnable r = () -> listeners.forEach(listener -> listener.environmentsListChanged(environments));
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }




    private void notifyEnvironmentChanged(String oldEnv, String newEnv) {
        Log.log(LOGGER::debug, "Firing EnvironmentChanged event for {}", newEnv);
        if (project.isDisposed()) {
            return;
        }
        NotificationUtil.notifyChangingEnvironment(project, oldEnv, newEnv);
        EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
        publisher.environmentChanged(newEnv);
    }

}
