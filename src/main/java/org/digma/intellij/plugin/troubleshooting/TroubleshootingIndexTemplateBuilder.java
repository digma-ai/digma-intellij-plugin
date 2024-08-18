package org.digma.intellij.plugin.troubleshooting;

import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import freemarker.template.*;
import org.digma.intellij.plugin.docker.DockerService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.ui.common.JaegerUtilKt;
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilderKt;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilderKt.IS_LOGGING_ENABLED;
import static org.digma.intellij.plugin.ui.jcef.JCEFUtilsKt.getIsLoggingEnabledSystemProperty;

class TroubleshootingIndexTemplateBuilder {

    private final Logger logger = Logger.getInstance(TroubleshootingIndexTemplateBuilder.class);

    private static final String INDEX_TEMPLATE_NAME = "troubleshooting.ftl";

    private static final String ENV_VARIABLE_IDE = "ide";
    private static final String USER_EMAIL_VARIABLE = "userEmail";
    private static final String IS_OBSERVABILITY_ENABLED_VARIABLE = "isObservabilityEnabled";
    private static final String IS_DOCKER_INSTALLED = "isDockerInstalled";
    private static final String IS_DOCKER_COMPOSE_INSTALLED = "isDockerComposeInstalled";
    private static final String IS_DIGMA_ENGINE_INSTALLED = "isDigmaEngineInstalled";
    private static final String IS_DIGMA_ENGINE_RUNNING = "isDigmaEngineRunning";
    private static final String IS_JAEGER_ENABLED = "isJaegerEnabled";

    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    public TroubleshootingIndexTemplateBuilder() {
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), TroubleshootingService.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }

    public InputStream build(Project project) {

        try {
            var data = new HashMap<String, Object>();
            BaseIndexTemplateBuilderKt.addCommonEnvVariables(data);

            data.put(ENV_VARIABLE_IDE, ApplicationNamesInfo.getInstance().getProductName());
            data.put(IS_JAEGER_ENABLED, JaegerUtilKt.isJaegerButtonEnabled());
            var userEmail = PersistenceService.getInstance().getUserEmail();
            data.put(USER_EMAIL_VARIABLE, userEmail == null ? "" : userEmail);
            data.put(IS_OBSERVABILITY_ENABLED_VARIABLE, PersistenceService.getInstance().isObservabilityEnabled());
            data.put(IS_DIGMA_ENGINE_INSTALLED, DockerService.getInstance().isEngineInstalled());
            data.put(IS_DIGMA_ENGINE_RUNNING, DockerService.getInstance().isEngineRunning(project));
            data.put(IS_DOCKER_INSTALLED, DockerService.getInstance().isDockerInstalled());
            data.put(IS_DOCKER_COMPOSE_INSTALLED, DockerService.getInstance().isDockerComposeInstalled());
            data.put(IS_LOGGING_ENABLED, getIsLoggingEnabledSystemProperty());

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
