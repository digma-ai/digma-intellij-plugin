package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Environment {

    private static final Logger LOGGER = Logger.getInstance(Environment.class);

    private final String baseUrl = "http://localhost:5051";

    private String current;

    private List<String> environments;

    private final Project project;
    private final AnalyticsService analyticsService;

    public Environment(Project project,AnalyticsService analyticsService) {
        this.project = project;
        this.analyticsService = analyticsService;
    }

    void loadEnvironments() {
        if (environments == null || environments.isEmpty()){
            environments = analyticsService.getEnvironments();
            if (environments != null && environments.size() > 0){
                Log.log(LOGGER::info, "Got environments {}", environments);
                current = environments.get(0);
            }else{
                Log.log(LOGGER::error, "Error loading environments: {}", environments);
                environments = new ArrayList<>();
            }
        }
    }


    String getBaseUrl() {
        return baseUrl;
    }

    public String getCurrent() {
        loadEnvironments();
        return current;
    }

    /**
     * Called when user changes environment
     * @param newEnv the new environment
     */
    public void setCurrent(String newEnv) {
        //don't change or fire the event if it's the same env. it happens because we have two combobox, one on each tab
        if (this.current != null && this.current.equals(newEnv)){
            return;
        }
        this.current = newEnv;
        notifyEnvironmentChanged(newEnv);
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
