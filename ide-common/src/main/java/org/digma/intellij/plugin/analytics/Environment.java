package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Environment {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);

    private String current;

    private List<String> environments;

    private final AnalyticsProvider analyticsProvider;
    private Project project;

    public Environment(Project project) {
        this.project = project;
        analyticsProvider = project.getService(AnalyticsProvider.class);
        loadEnvironments();
    }

    @Nullable
    private void loadEnvironments() {
        if (environments == null || environments.isEmpty()){
            environments = analyticsProvider.getEnvironments();
            if (environments != null && environments.size() > 0){
                Log.log(LOGGER::info, "Got environments {}", environments);
                current = environments.get(0);
            }else{
                Log.log(LOGGER::error, "Error loading environments: {}", environments);
                environments = new ArrayList<>();
            }
        }
    }


    public String getCurrent() {
        loadEnvironments();
        return current;
    }

    /**
     * Called when user changes environment
     * @param current the new environment
     */
    public void setCurrent(String current) {
        this.current = current;
        notifyEnvironmentChanged(current);
    }


    public void notifyEnvironmentChanged(String newEnv) {
        EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
        publisher.environmentChanged(newEnv);
    }




    @NotNull
    public List<String> getEnvironments() {
        loadEnvironments();
        return environments;
    }


}
