package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Environment {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);

    @Nullable
    private Env current;

    @NotNull
    private List<Env> environments = new ArrayList<>();

    //used to try restore selected environment on startup and after connections are lost and regained
    @Nullable
    private String latestKnownEnvId;

    private final Project project;
    private final AnalyticsService analyticsService;

    private final ReentrantLock envChangeLock = new ReentrantLock();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService) {
        this.project = project;
        this.analyticsService = analyticsService;
        latestKnownEnvId = PersistenceService.getInstance().getLatestSelectedEnvId();
    }


    //most methods are protected. don't use this object directly, use EnvUtils.kt


    @Nullable
    Env getCurrent() {
        return current;
    }


    void setCurrentById(@Nullable String envId) {

        if (StringUtils.isEmpty(envId)) {
            return;
        }

        setCurrentById(envId, null);
    }


    //this method does not handle illegal or null environment. it should be called with a non-null newEnv
    // that exists in the DB.
    void setCurrentById(@NotNull String envId, @Nullable Runnable taskToRunAfterChange) {

        try {
            Log.log(LOGGER::debug, "Setting current environment by id , old={},new={}", current, envId);

            if (StringUtils.isEmpty(envId)) {
                Log.log(LOGGER::debug, "setCurrent was called with an empty environment {}", envId);
                return;
            }

            //this setCurrentById method may send an env that does not exist in the
            // list of environments. so refresh if necessary.
            Runnable task = () -> {
                envChangeLock.lock();
                try {
                    //run both refreshEnvironments and updateCurrentEnv under same lock
                    if (environments.isEmpty() || !contains(envId)) {
                        refreshEnvironments();
                    }
                    updateCurrentEnv(envId);
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

            Backgroundable.ensureBackground(project, "Digma: environment changed " + envId, task);

        } catch (Throwable e) {
            ErrorReporter.getInstance().reportError(project, "Environment.setCurrent", e);
        }
    }


    @NotNull
    List<Env> getEnvironments() {
        return environments;
    }


    void refreshNowOnBackground() {

        Log.log(LOGGER::trace, "Refreshing Environments on background thread.");
        Backgroundable.ensureBackground(project, "Refreshing Environments", () -> {
            envChangeLock.lock();
            try {
                //run both refreshEnvironments and updateCurrentEnv under same lock
                refreshEnvironments();
                updateCurrentEnv(latestKnownEnvId);
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
            List<Env> envsFromBackend = analyticsService.getEnvironments();

            if (envsFromBackend.isEmpty()) {
                Log.log(LOGGER::trace, "Error loading environments or no environments added yet: {}", envsFromBackend);
                envsFromBackend = new ArrayList<>();
            } else {
                Log.log(LOGGER::trace, "Got environments {}", envsFromBackend);
            }


            if (collectionsEquals(envsFromBackend, environments)) {
                return;
            }

            this.environments = envsFromBackend;
            notifyEnvironmentsListChange();

        } finally {

            if (envChangeLock.isHeldByCurrentThread()) {
                envChangeLock.unlock();
            }

            stopWatch.stop();
            Log.log(LOGGER::trace, "Refresh environments took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }


    private void updateCurrentEnv(@Nullable String preferred) {

        var oldEnv = current;

        var optionalEnv = find(preferred);
        if (optionalEnv.isPresent()) {
            current = optionalEnv.get();
        } else if (current == null) {
            current = environments.isEmpty() ? null : environments.get(0);
        }

        if (current != null) {
            latestKnownEnvId = current.getId();
            PersistenceService.getInstance().setLatestSelectedEnvId(latestKnownEnvId);
        } else {
            PersistenceService.getInstance().setLatestSelectedEnvId(null);
        }

        if (!Objects.equals(oldEnv, current)) {
            notifyEnvironmentChanged(oldEnv, current);
        }
    }

    @Nullable
    public Env findById(@NotNull String envId) {
        return find(envId).orElse(null);
    }

    private Optional<Env> find(@Nullable String envIdToFind) {
        return environments.stream().filter(env -> env.getId().equals(envIdToFind)).findFirst();
    }

    private boolean contains(String envIdToFind) {
        return environments.stream().anyMatch(env -> env.getId().equals(envIdToFind));
    }


    private boolean collectionsEquals(Collection<Env> envs1, Collection<Env> envs2) {
        if (envs1 == null && envs2 == null) {
            return true;
        }

        if (envs1 != null && envs2 != null && envs1.size() == envs2.size()) {
            return new HashSet<>(envs1).containsAll(envs2);
        }

        return false;
    }


    private void notifyEnvironmentsListChange() {
        if (project.isDisposed()) {
            return;
        }
        Log.log(LOGGER::trace, "Firing EnvironmentsListChange event for {}", environments);

        //run in new background thread so locks can be freed because this method is called under lock
        Backgroundable.runInNewBackgroundThread(project, "environmentsListChanged", () -> {
            EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
            publisher.environmentsListChanged(environments);
        });

    }


    private void notifyEnvironmentChanged(Env oldEnv, Env newEnv) {
        if (project.isDisposed()) {
            return;
        }
        Log.log(LOGGER::trace, "Firing EnvironmentChanged event for {}", newEnv);

        Log.log(LOGGER::info, "Digma: Changing environment " + oldEnv + " to " + newEnv);

        //run in new background thread so locks can be freed because this method is called under lock
        Backgroundable.runInNewBackgroundThread(project, "environmentChanged", () -> {
            EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
            publisher.environmentChanged(newEnv);
        });
    }


}
