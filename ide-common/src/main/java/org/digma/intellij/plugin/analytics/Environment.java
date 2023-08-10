package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceData;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.digma.intellij.plugin.analytics.EnvironmentRefreshSchedulerKt.scheduleEnvironmentRefresh;

public class Environment implements EnvironmentsSupplier {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);
    private static final String NO_ENVIRONMENTS_MESSAGE = "No Environments";

    private String current;

    @NotNull
    private List<String> environments = new ArrayList<>();

    private final Project project;
    private final AnalyticsService analyticsService;
    private final PersistenceData persistenceData;

    private final ReentrantLock envChangeLock = new ReentrantLock();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService, @NotNull PersistenceData persistenceData) {
        this.project = project;
        this.analyticsService = analyticsService;
        this.persistenceData = persistenceData;
        this.current = persistenceData.getCurrentEnv();
        scheduleEnvironmentRefresh(analyticsService, this);

        //call refresh on environment when connection is lost, in some cases its necessary for some components to reset or update ui.
        //usually these components react to environment change events, so this will trigger an environment change if not already happened before.
        //if the connection lost happened during environment refresh then it may cause a second redundant event but will do no harm.
        project.getMessageBus().connect(analyticsService).subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, new AnalyticsServiceConnectionEvent() {
            @Override
            public void connectionLost() {
                Log.log(LOGGER::warn, "connectionLost");
                refreshNowOnBackground();
            }

            @Override
            public void connectionGained() {
                Log.log(LOGGER::warn, "connectionGained");
                refreshNowOnBackground();
            }
        });
    }


    @Override
    public String getCurrent() {
        return current;
    }

    @Override
    public void setCurrent(@Nullable String newEnv) {
        Log.log(LOGGER::warn, "setCurrent 1 " + newEnv);

        if (StringUtils.isEmpty(newEnv) || NO_ENVIRONMENTS_MESSAGE.equals(newEnv)) {
            return;
        }

        setCurrent(newEnv, true, null);
    }


    //this method does not handle illegal or null environment. it should be called with a non-null newEnv
    // that exists in the DB.
    @Override
    public void setCurrent(@NotNull String newEnv, boolean refreshInsightsView, @Nullable Runnable taskToRunAfterChange) {
        Log.test(LOGGER, "setCurrent 2 " + newEnv);

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
    }


    @NotNull
    @Override
    public List<String> getEnvironments() {
        return environments;
    }


    @Override
    public void refreshNowOnBackground() {

        Log.test(LOGGER, "Firing Refreshing Environments");
        Backgroundable.ensureBackground(project, "Refreshing Environments", () -> {
            Log.test(LOGGER, "Going to lock envChangeLock");
            envChangeLock.lock();
            Log.test(LOGGER, "envChangeLock locked");
            try {
                //run both refreshEnvironments and updateCurrentEnv under same lock
                Log.test(LOGGER, "call refreshEnvironments");
                refreshEnvironments();
                Log.test(LOGGER, "call updateCurrentEnv");
                updateCurrentEnv(persistenceData.getCurrentEnv(), true);
            } finally {
                Log.test(LOGGER, "Going to unlock envChangeLock");
                if (envChangeLock.isHeldByCurrentThread()) {
                    envChangeLock.unlock();
                }
                Log.test(LOGGER, "envChangeLock unlocked");
            }
        });
    }

    void refreshNow() {

        Log.test(LOGGER, "Going to lock envChangeLock");
        envChangeLock.lock();
        Log.test(LOGGER, "envChangeLock locked");
        try {
            //run both refreshEnvironments and updateCurrentEnv under same lock
            Log.test(LOGGER, "Calling refreshEnvironments");
            refreshEnvironments();
            Log.test(LOGGER, "Calling updateCurrentEnv");
            updateCurrentEnv(persistenceData.getCurrentEnv(), true);
        } finally {
            Log.test(LOGGER, "Going to unlock envChangeLock");
            if (envChangeLock.isHeldByCurrentThread()) {
                envChangeLock.unlock();
                Log.test(LOGGER, "envChangeLock unlocked");
            }
        }
    }


    //this method should not be called on ui threads, it may hang and cause a freeze
    private void refreshEnvironments() {
        var stopWatch = StopWatch.createStarted();

        Log.test(LOGGER, "Going to lock envChangeLock");
        envChangeLock.lock();
        Log.test(LOGGER, "envChangeLock locked");
        try {
            Log.test(LOGGER, "Calling analyticsService.getEnvironments()");
            var newEnvironments = analyticsService.getEnvironments();
            if (newEnvironments != null && !newEnvironments.isEmpty()) {
                Log.test(LOGGER, "Got environments {}", newEnvironments);
            } else {
                Log.test(LOGGER, "Error loading environments: {}", newEnvironments);
                newEnvironments = new ArrayList<>();
            }

            if (environmentsListEquals(newEnvironments, environments)) {
                return;
            }

            this.environments = newEnvironments;
            notifyEnvironmentsListChange();

        } finally {
            Log.test(LOGGER, "Going to unlock envChangeLock");
            if (envChangeLock.isHeldByCurrentThread()) {
                envChangeLock.unlock();
                Log.test(LOGGER, "envChangeLock unlocked");
            }
            stopWatch.stop();
            Log.test(LOGGER, "Refresh environments took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }


    private void updateCurrentEnv(@Nullable String preferred, boolean refreshInsightsView) {
        Log.test(LOGGER, "updateCurrentEnv");

        var oldEnv = current;

        if (preferred != null && this.environments.contains(preferred)) {
            current = preferred;
        } else if (current == null || !this.environments.contains(current)) {
            current = environments.isEmpty() ? null : environments.get(0);
        }

        if (current != null) {
            //don't update the persistent data with null,null will happen on connection lost,
            //so when connection is back the current env can be restored to the last one.
            persistenceData.setCurrentEnv(current);
        }

        if (!Objects.equals(oldEnv, current)) {
            notifyEnvironmentChanged(oldEnv, current, refreshInsightsView);
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


    private void notifyEnvironmentsListChange() {
        Log.test(LOGGER, "Going to fire environmentsListChanged for {}", environments);
        if (project.isDisposed()) {
            return;
        }

        Log.test(LOGGER, "Firing environmentsListChanged for {}", environments);
        //run in new background thread so locks can be freeied because this method is called under lock
        Backgroundable.runInNewBackgroundThread(project, "environmentsListChanged", () -> {
            Log.test(LOGGER, "environmentsListChanged for {}", environments);
            EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
            publisher.environmentsListChanged(environments);
        });

    }


    private void notifyEnvironmentChanged(String oldEnv, String newEnv, boolean refreshInsightsView) {
        Log.test(LOGGER, "Going to fire environmentChanged for {}", environments);
        if (project.isDisposed()) {
            return;
        }

        Log.test(LOGGER, "Firing environmentChanged for {}", newEnv);
        //run in new background thread so locks can be freed because this method is called under lock
        Backgroundable.runInNewBackgroundThread(project, "environmentChanged", () -> {
            Log.test(LOGGER, "!!!!!!!!!!!!!environmentChanged for {}", newEnv);
            EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
            publisher.environmentChanged(newEnv, refreshInsightsView);
        });
    }

}
