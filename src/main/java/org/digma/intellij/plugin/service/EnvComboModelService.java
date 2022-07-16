package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.ui.model.environment.EnvComboModel;
import org.jetbrains.annotations.NotNull;


/**
 * the purpose of this service is to initialize and hold the shared environment combo model
 */
public class EnvComboModelService{

    private final EnvComboModel envComboModel;

    public EnvComboModelService(@NotNull Project project) {
        var analyticsService = project.getService(AnalyticsService.class);
        var env = analyticsService.getEnvironment();
        envComboModel = new EnvComboModel(env);
    }


    public EnvComboModel getEnvComboModel() {
        return envComboModel;
    }
}
