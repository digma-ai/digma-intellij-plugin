package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Environment {

    private String current;

    private final List<String> environments;

    private final AnalyticsProvider analyticsProvider;
    private Project project;

    public Environment(Project project) {
        this.project = project;
        analyticsProvider = project.getService(AnalyticsProvider.class);
        environments = analyticsProvider.getEnvironments();
        if (environments != null && environments.size() > 0){
            current = environments.get(0);
        }
    }

    public String getCurrent() {
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
        return environments;
    }


}
