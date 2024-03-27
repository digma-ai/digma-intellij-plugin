package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.env.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Environment implements EnvironmentsSupplier {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);

    private Env current;

    @NotNull
    private List<Env> environments = new ArrayList<>();

    //used to try restore environment after connections are lost and regained
    private String latestKnownEnv = null;

    private final Project project;
    private final AnalyticsService analyticsService;

    private final ReentrantLock envChangeLock = new ReentrantLock();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService) {
        this.project = project;
        this.analyticsService = analyticsService;
        latestKnownEnv = PersistenceService.getInstance().getCurrentEnv();
    }


    @Override
    @Nullable
    public Env getCurrent() {
        return current;
    }

    @Override
    @NotNull
    public String getLatestKnownEnv() {
        return latestKnownEnv;
    }


    @Override
    public void setCurrent(@NotNull Env env) {
        setCurrent(env.getOriginalName());
    }


    @Override
    public void setCurrent(@Nullable String newEnv) {

        if (StringUtils.isEmpty(newEnv)) {
            return;
        }

        setCurrent(newEnv, null);
    }


    //this method does not handle illegal or null environment. it should be called with a non-null newEnv
    // that exists in the DB.
    @Override
    public void setCurrent(@NotNull String newEnv, @Nullable Runnable taskToRunAfterChange) {

        try {
            Log.log(LOGGER::debug, "Setting current environment , old={},new={}", this.current, newEnv);

            if (StringUtils.isEmpty(newEnv)) {
                Log.log(LOGGER::debug, "setCurrent was called with an empty environment {}", newEnv);
                return;
            }

            //this setCurrent method may send an env that does not exist in the
            // list of environments. so refresh if necessary.
            Runnable task = () -> {
                envChangeLock.lock();
                try {
                    //run both refreshEnvironments and updateCurrentEnv under same lock
                    if (environments.isEmpty() || !contains(newEnv)) {
                        refreshEnvironments();
                    }
                    updateCurrentEnv(newEnv);
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
            ErrorReporter.getInstance().reportError(project, "Environment.setCurrent", e);
        }
    }


    @NotNull
    @Override
    public List<Env> getEnvironments() {
        return environments;
    }

    @NotNull
    @Override
    public List<String> getEnvironmentsNames() {
        return getEnvironments().stream().map(Env::getOriginalName).toList();
    }


    @Override
    public void refreshNowOnBackground() {

        Log.log(LOGGER::trace, "Refreshing Environments on background thread.");
        Backgroundable.ensureBackground(project, "Refreshing Environments", () -> {
            envChangeLock.lock();
            try {
                //run both refreshEnvironments and updateCurrentEnv under same lock
                refreshEnvironments();
                updateCurrentEnv(latestKnownEnv);
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
            List<String> envsFromBackend = analyticsService.getRawEnvironments();

            if (envsFromBackend.isEmpty()) {
                Log.log(LOGGER::trace, "Error loading environments or no environments added yet: {}", envsFromBackend);
                envsFromBackend = new ArrayList<>();
            } else {
                Log.log(LOGGER::trace, "Got environments {}", envsFromBackend);
            }


            var newEnvironments = envsFromBackend.stream().map(Env::toEnv).toList();


            if (collectionsEquals(newEnvironments, environments)) {
                return;
            }

            this.environments = newEnvironments;
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
            latestKnownEnv = current.getOriginalName();
            PersistenceService.getInstance().setCurrentEnv(current.getOriginalName());
        } else {
            PersistenceService.getInstance().setCurrentEnv(null);
        }

        if (!Objects.equals(oldEnv, current)) {
            notifyEnvironmentChanged(oldEnv, current);
        }
    }

    private Optional<Env> find(@Nullable String envToFind) {
        return environments.stream().filter(env -> env.getOriginalName().equals(envToFind)).findFirst();
    }

    private boolean contains(String envToFind) {
        return environments.stream().anyMatch(env -> env.getOriginalName().equals(envToFind));
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


    private void notifyEnvironmentChanged(Env oldEnv, Env newEnv) {
        Log.log(LOGGER::trace, "Firing EnvironmentChanged event for {}", newEnv);
        if (project.isDisposed()) {
            return;
        }

        Log.log(LOGGER::info, "Digma: Changing environment " + oldEnv + " to " + newEnv);

        //run in new background thread so locks can be freed because this method is called under lock
        Backgroundable.runInNewBackgroundThread(project, "environmentChanged", () -> {
            EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
            publisher.environmentChanged(newEnv);
        });
    }


}
