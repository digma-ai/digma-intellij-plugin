package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.digma.intellij.plugin.docker.DockerService;
import org.digma.intellij.plugin.jcef.common.JCefTemplateUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

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
    private static final String DASHBOARD_REFRESH_INTERVAL = "dashboardRefreshInterval";
    private static final String DASHBOARD_ENVIRONMENT = "dashboardEnvironment";
    private static final String DIGMA_API_URL = "digmaApiUrl";

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

            JCefTemplateUtils.addCommonEnvVariables(data);

            data.put(ENV_VARIABLE_IDE, ApplicationNamesInfo.getInstance().getProductName());
            data.put(IS_JAEGER_ENABLED, JaegerUtilKt.isJaegerButtonEnabled());
            var userEmail = PersistenceService.getInstance().getUserEmail();
            data.put(USER_EMAIL_VARIABLE, userEmail == null ? "" : userEmail);
            data.put(IS_OBSERVABILITY_ENABLED_VARIABLE, PersistenceService.getInstance().isAutoOtel());
            data.put(IS_DIGMA_ENGINE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isEngineInstalled());
            data.put(IS_DIGMA_ENGINE_RUNNING, ApplicationManager.getApplication().getService(DockerService.class).isEngineRunning(project));
            data.put(IS_DOCKER_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());
            data.put(IS_DOCKER_COMPOSE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());
            data.put(DIGMA_API_URL, SettingsState.getInstance().apiUrl);

            data.put(DASHBOARD_REFRESH_INTERVAL, 10*1000);
            data.put(DASHBOARD_ENVIRONMENT, PersistenceService.getInstance().getCurrentEnv());

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
