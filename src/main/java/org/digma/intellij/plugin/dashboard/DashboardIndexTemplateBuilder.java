package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import freemarker.template.*;
import org.digma.intellij.plugin.docker.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.common.JaegerUtilKt;
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilderKt;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.digma.intellij.plugin.analytics.EnvUtilsKt.getCurrentEnvironmentId;
import static org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilderKt.IS_LOGGING_ENABLED;
import static org.digma.intellij.plugin.ui.jcef.JCEFUtilsKt.getIsLoggingEnabledSystemProperty;

public class DashboardIndexTemplateBuilder {
    private final Logger logger = Logger.getInstance(DashboardIndexTemplateBuilder.class);

    private static final String INDEX_TEMPLATE_NAME = "dashboard.ftl";
    private static final String ENV_VARIABLE_IDE = "ide";
    private static final String USER_EMAIL_VARIABLE = "userEmail";
    private static final String IS_OBSERVABILITY_ENABLED_VARIABLE = "isObservabilityEnabled";
    private static final String IS_DOCKER_INSTALLED = "isDockerInstalled";
    private static final String IS_DOCKER_COMPOSE_INSTALLED = "isDockerComposeInstalled";
    private static final String IS_DIGMA_ENGINE_INSTALLED = "isDigmaEngineInstalled";
    private static final String IS_DIGMA_ENGINE_RUNNING = "isDigmaEngineRunning";
    private static final String IS_JAEGER_ENABLED = "isJaegerEnabled";
    private static final String DASHBOARD_ENVIRONMENT = "dashboardEnvironment";
    private static final String DIGMA_API_URL = "digmaApiUrl";
    private static final String INITIAL_ROUTE_PARAM_NAME = "initial_route";

    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    public DashboardIndexTemplateBuilder() {
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), DashboardFileEditor.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }

    public InputStream build(Project project, DashboardVirtualFile dashboardVirtualFile) {

        try {
            var data = new HashMap<String, Object>();

            BaseIndexTemplateBuilderKt.addCommonEnvVariables(data);

            data.put(ENV_VARIABLE_IDE, ApplicationNamesInfo.getInstance().getProductName());
            data.put(IS_JAEGER_ENABLED, JaegerUtilKt.isJaegerButtonEnabled());
            var userEmail = PersistenceService.getInstance().getUserEmail();
            data.put(USER_EMAIL_VARIABLE, userEmail == null ? "" : userEmail);
            data.put(IS_OBSERVABILITY_ENABLED_VARIABLE, PersistenceService.getInstance().isObservabilityEnabled());
            data.put(IS_DIGMA_ENGINE_INSTALLED, LocalInstallationFacade.getInstance().isLocalEngineInstalled());
            data.put(IS_DIGMA_ENGINE_RUNNING, LocalInstallationFacade.getInstance().isLocalEngineRunning(project));
            data.put(IS_DOCKER_INSTALLED, DockerService.getInstance().isDockerInstalled());
            data.put(IS_DOCKER_COMPOSE_INSTALLED, DockerService.getInstance().isDockerComposeInstalled());
            data.put(DIGMA_API_URL, SettingsState.getInstance().getApiUrl());
            var envId = getCurrentEnvironmentId(project);
            data.put(DASHBOARD_ENVIRONMENT, envId == null ? "undefined" : envId);
            data.put(IS_LOGGING_ENABLED, getIsLoggingEnabledSystemProperty());
            data.put(INITIAL_ROUTE_PARAM_NAME, dashboardVirtualFile.getInitialRoute());
            Template template = freemarketConfiguration.getTemplate(INDEX_TEMPLATE_NAME);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            return new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            Log.debugWithException(logger, e, "error creating template for index.html");
            return null;
        }
    }

}
