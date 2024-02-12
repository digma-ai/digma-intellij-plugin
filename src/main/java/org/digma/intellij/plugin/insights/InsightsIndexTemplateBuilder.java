package org.digma.intellij.plugin.insights;

import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import freemarker.template.*;
import org.digma.intellij.plugin.docker.DockerService;
import org.digma.intellij.plugin.jcef.common.JCefTemplateUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt.getJaegerUrl;

class InsightsIndexTemplateBuilder {

    private final Logger logger = Logger.getInstance(InsightsIndexTemplateBuilder.class);

    private static final String INDEX_TEMPLATE_NAME = "insightstemplate.ftl";

    private static final String ENV_VARIABLE_IDE = "ide";
    private static final String USER_EMAIL_VARIABLE = "userEmail";
    private static final String USER_REGISTRATION_EMAIL_VARIABLE = "userRegistrationEmail";
    private static final String JAEGER_URL_VARIABLE = "jaegerURL";

    private static final String IS_OBSERVABILITY_ENABLED_VARIABLE = "isObservabilityEnabled";
    private static final String IS_DOCKER_INSTALLED = "isDockerInstalled";
    private static final String IS_DOCKER_COMPOSE_INSTALLED = "isDockerComposeInstalled";
    private static final String IS_DIGMA_ENGINE_INSTALLED = "isDigmaEngineInstalled";
    private static final String IS_DIGMA_ENGINE_RUNNING = "isDigmaEngineRunning";
    private static final String IS_JAEGER_ENABLED = "isJaegerEnabled";


    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    public InsightsIndexTemplateBuilder() {
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), InsightsServiceImpl.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }

    public InputStream build(Project project) {

        try {
            var data = new HashMap<String, Object>();
            JCefTemplateUtils.addCommonEnvVariables(data);
            JCefTemplateUtils.addIsJaegerEnabled(data);

            data.put(ENV_VARIABLE_IDE, ApplicationNamesInfo.getInstance().getProductName());
            data.put(IS_JAEGER_ENABLED, JaegerUtilKt.isJaegerButtonEnabled());
            var userEmail = PersistenceService.getInstance().getUserEmail();
            data.put(USER_EMAIL_VARIABLE, userEmail == null ? "" : userEmail);
            var userRegistrationEmail = PersistenceService.getInstance().getUserRegistrationEmail();
            data.put(USER_REGISTRATION_EMAIL_VARIABLE, userRegistrationEmail == null ? "" : userRegistrationEmail);
            var jaegerUrl = getJaegerUrl();
            data.put(JAEGER_URL_VARIABLE, jaegerUrl == null ? "" : jaegerUrl);
            data.put(IS_OBSERVABILITY_ENABLED_VARIABLE, PersistenceService.getInstance().isObservabilityEnabled());
            data.put(IS_DIGMA_ENGINE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isEngineInstalled());
            data.put(IS_DIGMA_ENGINE_RUNNING, ApplicationManager.getApplication().getService(DockerService.class).isEngineRunning(project));
            data.put(IS_DOCKER_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());
            data.put(IS_DOCKER_COMPOSE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());



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
