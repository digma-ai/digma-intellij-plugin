package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceData;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Environment implements EnvironmentsSupplier {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);
    private static final String NO_ENVIRONMENTS_MESSAGE = "No Environments";

    private String current;
    private final SettingsState settingsState;

    @NotNull
    private List<String> environments = new ArrayList<>();

    private final Project project;
    private final AnalyticsService analyticsService;
    private final PersistenceData persistenceData;

    private Instant lastRefreshTimestamp = Instant.now();

    private final ReentrantLock envChangeLock = new ReentrantLock();

    public Environment(@NotNull Project project, @NotNull AnalyticsService analyticsService, @NotNull PersistenceData persistenceData, SettingsState settingsState) {
        this.project = project;
        this.analyticsService = analyticsService;
        this.persistenceData = persistenceData;
        this.current = persistenceData.getCurrentEnv();
        this.settingsState = settingsState;

        //call refresh on environment when connection is lost, in some cases its necessary for some components to reset or update ui.
        //usually these components react to environment change events, so this will trigger an environment change if not already happened before.
        //if the connection lost happened during environment refresh then it may cause a second redundant event but will do no harm.
        project.getMessageBus().connect(analyticsService).subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, new AnalyticsServiceConnectionEvent() {
            @Override
            public void connectionLost() {
                refresh();
            }

            @Override
            public void connectionGained() {
                if (getCurrent() == null || getCurrent().isEmpty()) {
                    refreshNowOnBackground();
                }
            }
        });
    }



    @Override
    public String getCurrent() {
        return current;
    }

    @Override
    public void setCurrent(@NotNull String newEnv) {

        Log.log(LOGGER::debug, "Setting current environment , old={},new={}", this.current, newEnv);

        //don't change or fire the event if it's the same env.
        if (Objects.equals(this.current, newEnv) || StringUtils.isEmpty(newEnv) || NO_ENVIRONMENTS_MESSAGE.equals(newEnv)) {
            return;
        }


        //changeEnvironment must run in the background, the use of envChangeLock makes sure no two threads
        //change environment at the same time. if user changes environments very quickly they will run one after the
        // other.
        //listeners that handle environmentChanged event in most cases need to stay on the same thread.
        Runnable task = () -> {
            envChangeLock.lock();
            try {
                if (environments.isEmpty() || !environments.contains(newEnv)) {
                    // we got here to changeEnv but list is empty, probably first run/call
                    refreshEnvironments();
                }
                changeEnvironment(newEnv);
            } finally {
                envChangeLock.unlock();
            }
        };

        Backgroundable.ensureBackground(project, "Digma: environment changed " + newEnv, task);
    }


    private void changeEnvironment(@NotNull String newEnv) {
        if (StringUtils.isNotEmpty(newEnv)) {
            var oldEnv = this.current;
            this.current = newEnv;
            persistenceData.setCurrentEnv(newEnv);
            notifyEnvironmentChanged(oldEnv, newEnv);
        }
    }


    @NotNull
    @Override
    public List<String> getEnvironments() {
        return environments;
    }


    //refresh is called many times, every time the method context changes,and it's not necessary to
    //really try every time,its used as a hook to refresh in case the backend list changed.
    //so it will only refresh if some time passed since the last call
    @Override
    public void refresh() {
        refreshOnlyEverySomeTimePassed();
    }


    //this should be used when a refresh is necessary as soon as possible.
    @Override
    public void refreshNowOnBackground() {
        refreshOnBackground();
    }

    private void refreshOnlyEverySomeTimePassed() {

        if (!timeToRefresh()) {
            Log.log(LOGGER::debug, "Skipping Refresh Environments , will try again in few seconds..");
            return;
        }

        refreshOnBackground();
    }


    private void refreshOnBackground() {
        Log.log(LOGGER::debug, "Refreshing Environments on background thread.");
        Backgroundable.ensureBackground(project, "Refreshing Environments on background thread.", this::refreshEnvironments);
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



    //this method should not be called on ui threads, it may hang and cause a freeze
    void refreshEnvironments() {

        var stopWatch = StopWatch.createStarted();

        try {
            Log.log(LOGGER::debug, "Refresh Environments called");

            Log.log(LOGGER::debug, "Refreshing Environments list");
            var newEnvironments = analyticsService.getEnvironments();
            if (newEnvironments != null && !newEnvironments.isEmpty()) {
                Log.log(LOGGER::debug, "Got environments {}", newEnvironments);
            } else {
                Log.log(LOGGER::warn, "Error loading environments: {}", newEnvironments);
                newEnvironments = new ArrayList<>();
            }

            if (environmentsListEquals(newEnvironments, environments)) {
                return;
            }

            replaceEnvironmentsListAndFireChange(newEnvironments);
        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "Refresh environments took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }


    void replaceEnvironmentsList(@NotNull List<String> envs) {
        this.environments = envs;

        if (this.environments.contains(persistenceData.getCurrentEnv())) {
            current = persistenceData.getCurrentEnv();
        } else if (current == null || !this.environments.contains(current)) {
            current = environments.isEmpty() ? null : environments.get(0);
        }

        if (current != null) {
            //don't update the persistent data with null,null will happen on connection lost,
            //so when connection is back the current env can be restored to the last one.
            persistenceData.setCurrentEnv(current);
        }
    }


    //this method may be called from both ui threads or background threads
    void replaceEnvironmentsListAndFireChange(@NotNull List<String> envs) {
        Log.log(LOGGER::debug, "replaceEnvironmentsListAndFireChange called");

        var oldEnv = current;

        replaceEnvironmentsList(envs);

        notifyEnvironmentsListChange();

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


    private void notifyEnvironmentsListChange() {
        Log.log(LOGGER::debug, "Firing EnvironmentsListChange event for {}", environments);
        if (project.isDisposed()) {
            return;
        }
        EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
        publisher.environmentsListChanged(environments);
    }

    private void notifyEnvironmentChanged(String oldEnv, String newEnv) {
        Log.log(LOGGER::debug, "Firing EnvironmentChanged event for {}", newEnv);
        if (project.isDisposed()) {
            return;
        }
        Log.log(LOGGER::info, "Digma: Changing environment " + oldEnv + " to " + newEnv);
        EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
        publisher.environmentChanged(newEnv);
    }

}
