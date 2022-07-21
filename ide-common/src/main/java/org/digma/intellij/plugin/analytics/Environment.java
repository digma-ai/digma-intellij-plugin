package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.notifications.NotificationUtil;
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
    private final PersistenceData persistenceData;

    private final Set<EnvironmentsListChangedListener> listeners = new LinkedHashSet<>();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService, @NotNull PersistenceData persistenceData) {
        this.project = project;
        this.analyticsService = analyticsService;
        this.persistenceData = persistenceData;
        this.current = persistenceData.getCurrentEnv();
    }

    private void maybeRecoverEnvironments() {
        if (environments == null || environments.isEmpty()) {
            Log.log(LOGGER::debug, "Environments list is empty,trying recovery");
            refreshEnvironments();
        }
    }

    private void refreshEnvironments() {
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


        this.environments = newEnvironments;

        fireEnvironmentsListChange();

        maybeUpdateCurrent();

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


    @Override
    public String getCurrent() {
        maybeRecoverEnvironments();
        return current;
    }

    /**
     * Called when user changes environment
     *
     * @param newEnv the new environment
     */
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


    private void notifyEnvironmentChanged(String oldEnv, String newEnv) {
        Log.log(LOGGER::debug, "Firing EnvironmentChanged event for {}", newEnv);
        if (project.isDisposed()) {
            return;
        }
        NotificationUtil.notifyChangingEnvironment(project, oldEnv, newEnv);
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


    private void maybeUpdateCurrent() {
        if (current == null || current.isBlank() || !environments.contains(current)) {
            var oldEnv = this.current;
            current = environments.size() > 0 ? environments.get(0) : null;
            persistenceData.setCurrentEnv(current);
            notifyEnvironmentChanged(oldEnv, current);
        }
    }

    @Override
    public void refresh() {
        new Task.Backgroundable(project, "Refreshing environments") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                refreshEnvironments();
            }
        }.queue();

    }
}
