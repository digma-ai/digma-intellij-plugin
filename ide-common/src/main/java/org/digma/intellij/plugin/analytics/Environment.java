package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceData;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsListChangedListener;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

public class Environment implements EnvironmentsSupplier {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);

    private String current;

    @NotNull
    private List<String> environments = new ArrayList<>();

    private final Project project;
    private final AnalyticsService analyticsService;
    private PersistenceData persistenceData;

    private final Set<EnvironmentsListChangedListener> listeners = new LinkedHashSet<>();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService, @NotNull PersistenceData persistenceData) {
        this.project = project;
        this.analyticsService = analyticsService;
        this.persistenceData = persistenceData;
        this.current = persistenceData.getCurrentEnv();
    }

    void maybeRecoverEnvironments() {
        if (environments == null || environments.isEmpty()) {
            Log.log(LOGGER::debug, "Environments list is empty,trying recovery");
            refreshEnvironments();
        }
    }

    void refreshEnvironments() {
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


        setEnvironmentsList(newEnvironments);

        fireEnvironmentsListChange();
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
        SwingUtilities.invokeLater(() -> listeners.forEach(listener -> listener.environmentsListChanged(environments)));
    }


    @Override
    public String getCurrent() {
        maybeRecoverEnvironments();
        return current;
    }

    /**
     * Called when user changes environment
     * @param newEnv the new environment
     */
    @Override
    public void setCurrent(String newEnv) {

        Log.log(LOGGER::debug, "Setting current environment , old={},new={}", this.current, newEnv);

        //don't change or fire the event if it's the same env. it happens because we have two combobox, one on each tab

        if (this.current == null && newEnv == null) {
            return;
        }

        if (this.current != null && this.current.equals(newEnv)) {
            return;
        }

        this.current = newEnv;
        persistenceData.setCurrentEnv(newEnv);

        notifyEnvironmentChanged(newEnv);
    }


    public void notifyEnvironmentChanged(String newEnv) {
        Log.log(LOGGER::debug, "Firing EnvironmentChanged event for {}", newEnv);
        EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
        publisher.environmentChanged(newEnv);
    }


    @NotNull
    @Override
    public List<String> getEnvironments() {
        maybeRecoverEnvironments();
        return environments;
    }


    @Override
    public void addEnvironmentsListChangeListener(EnvironmentsListChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            listeners.add(listener);
        }
    }

    void setEnvironmentsList(@NotNull List<String> envs) {
        this.environments = envs;
        maybeUpdateCurrent();
    }


    private void maybeUpdateCurrent() {
        if (current == null || current.isBlank() || !environments.contains(current)){
            current = environments.size() > 0 ? environments.get(0) : null;
            persistenceData.setCurrentEnv(current);
            notifyEnvironmentChanged(current);
        }
    }

    @Override
    public void refresh() {
        refreshEnvironments();
    }
}
