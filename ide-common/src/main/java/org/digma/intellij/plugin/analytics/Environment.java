package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.usage.CodeObjectUsageStatus;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Environment implements EnvironmentsSupplier {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);
    private static final String NO_ENVIRONMENTS_MESSAGE = "No Environments";

    private String current;

    @NotNull
    private List<String> environments = new ArrayList<>();

    @NotNull
    private Set<String> environmentsWithUsages = new HashSet<>();



    private final Project project;
    private final AnalyticsService analyticsService;

    private final ReentrantLock envChangeLock = new ReentrantLock();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService) {
        this.project = project;
        this.analyticsService = analyticsService;
    }


    @Override
    public String getCurrent() {
        return current;
    }

    @Override
    public void setCurrent(@Nullable String newEnv) {

        if (StringUtils.isEmpty(newEnv) || NO_ENVIRONMENTS_MESSAGE.equals(newEnv)) {
            return;
        }

        setCurrent(newEnv, true, null);
    }


    //this method does not handle illegal or null environment. it should be called with a non-null newEnv
    // that exists in the DB.
    @Override
    public void setCurrent(@NotNull String newEnv, boolean refreshInsightsView, @Nullable Runnable taskToRunAfterChange) {

        try {
            Log.log(LOGGER::debug, "Setting current environment , old={},new={}", this.current, newEnv);

            if (StringUtils.isEmpty(newEnv)) {
                Log.log(LOGGER::debug, "setCurrent was called with an empty environment {}", newEnv);
                return;
            }

            //this setCurrent method is called from RecentActivityService, it may send an env that does not exist in  the
            // list of environments. so refresh if necessary.
            Runnable task = () -> {
                envChangeLock.lock();
                try {
                    //run both refreshEnvironments and updateCurrentEnv under same lock
                    if (environments.isEmpty() || !environments.contains(newEnv)) {
                        refreshEnvironments();
                    }
                    updateCurrentEnv(newEnv, refreshInsightsView);
                } finally {
                    if (envChangeLock.isHeldByCurrentThread()) {
                        envChangeLock.unlock();
                    }
                }

                //runs in background but not under lock
                if (taskToRunAfterChange != null) {
                    taskToRunAfterChange.run();
                }

            };

            Backgroundable.ensureBackground(project, "Digma: environment changed " + newEnv, task);
        } catch (Throwable e) {
            ErrorReporter.getInstance().reportError("Environment.setCurrent", e);
        }
    }


    @NotNull
    @Override
    public List<String> getEnvironments() {
        return environments;
    }

    @Override
    public boolean hasUsages(@NotNull String env) {
        return environmentsWithUsages.contains(env);
    }

    @Override
    public void refreshNowOnBackground() {

        Log.log(LOGGER::trace, "Refreshing Environments on background thread.");
        Backgroundable.ensureBackground(project, "Refreshing Environments", () -> {
            envChangeLock.lock();
            try {
                //run both refreshEnvironments and updateCurrentEnv under same lock
                refreshEnvironments();
                updateCurrentEnv(PersistenceService.getInstance().getState().getCurrentEnv(), true);
            } catch (Exception e) {
                Log.warnWithException(LOGGER, e, "Exception in refreshNowOnBackground");
                ErrorReporter.getInstance().reportError(project, "Environment.refreshNowOnBackground", e);
            } finally {
                if (envChangeLock.isHeldByCurrentThread()) {
                    envChangeLock.unlock();
                }
            }
        });
    }


    //this method should not be called on ui threads, it may hang and cause a freeze
    private void refreshEnvironments() {

        var stopWatch = StopWatch.createStarted();

        envChangeLock.lock();

        try {
            Log.log(LOGGER::trace, "Refresh Environments called");

            Log.log(LOGGER::trace, "Refreshing Environments list");
            var newEnvironments = analyticsService.getEnvironments();
            var newEnvsWithUsages = getEnvsWithUsages();
            if (newEnvironments != null && !newEnvironments.isEmpty()) {
                Log.log(LOGGER::trace, "Got environments {}", newEnvironments);
            } else {
                Log.log(LOGGER::warn, "Error loading environments or no environments added yet: {}", newEnvironments);
                newEnvironments = new ArrayList<>();
            }

            if (collectionsEquals(newEnvironments, environments) && collectionsEquals(newEnvsWithUsages, environmentsWithUsages)) {
                return;
            }

            this.environments = newEnvironments;
            this.environmentsWithUsages = newEnvsWithUsages;
            notifyEnvironmentsListChange();

        } finally {

            if (envChangeLock.isHeldByCurrentThread()) {
                envChangeLock.unlock();
            }

            stopWatch.stop();
            Log.log(LOGGER::trace, "Refresh environments took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    private Set<String> getEnvsWithUsages() {

        try {
            var usageStatusResult = analyticsService.getEnvironmentsUsageStatus();
            return usageStatusResult.getCodeObjectStatuses().stream().map(CodeObjectUsageStatus::getEnvironment).collect(Collectors.toSet());
        } catch (AnalyticsServiceException e) {
            ErrorReporter.getInstance().reportError(project, "Environment.getEnvsWithUsages", e);
            return Collections.emptySet();
        }
    }


    private void updateCurrentEnv(@Nullable String preferred, boolean refreshInsightsView) {

        var oldEnv = current;

        if (preferred != null && this.environments.contains(preferred)) {
            current = preferred;
        } else if (current == null || !this.environments.contains(current)) {
            current = environments.isEmpty() ? null : environments.get(0);
        }

        if (current != null) {
            //don't update the persistent data with null,null will happen on connection lost,
            //so when connection is back the current env can be restored to the last one.
            PersistenceService.getInstance().getState().setCurrentEnv(current);
        }

        if (!Objects.equals(oldEnv, current)) {
            notifyEnvironmentChanged(oldEnv, current, refreshInsightsView);
        }
    }


    private boolean collectionsEquals(Collection<String> envs1, Collection<String> envs2) {
        if (envs1 == null && envs2 == null) {
            return true;
        }

        if (envs1 != null && envs2 != null && envs1.size() == envs2.size()) {
            return new HashSet<>(envs1).containsAll(envs2);
        }

        return false;
    }


    private void notifyEnvironmentsListChange() {
        Log.log(LOGGER::trace, "Firing EnvironmentsListChange event for {}", environments);
        if (project.isDisposed()) {
            return;
        }

        //run in new background thread so locks can be freed because this method is called under lock
        Backgroundable.runInNewBackgroundThread(project, "environmentsListChanged", () -> {
            EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
            publisher.environmentsListChanged(environments);
        });

    }


    private void notifyEnvironmentChanged(String oldEnv, String newEnv, boolean refreshInsightsView) {
        Log.log(LOGGER::trace, "Firing EnvironmentChanged event for {}", newEnv);
        if (project.isDisposed()) {
            return;
        }

        Log.log(LOGGER::info, "Digma: Changing environment " + oldEnv + " to " + newEnv);

        //run in new background thread so locks can be freed because this method is called under lock
        Backgroundable.runInNewBackgroundThread(project, "environmentChanged", () -> {
            EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
            publisher.environmentChanged(newEnv, refreshInsightsView);
        });
    }


    //this method is for internal use of the package
    void notifyChange(String oldEnv) {
        notifyEnvironmentsListChange();
        notifyEnvironmentChanged(oldEnv,current,true);
    }


}
