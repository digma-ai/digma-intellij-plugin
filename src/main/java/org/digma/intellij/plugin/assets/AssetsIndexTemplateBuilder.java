package org.digma.intellij.plugin.assets;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.text.VersionComparatorUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.digma.intellij.plugin.docker.DockerService;
import org.digma.intellij.plugin.jcef.common.JCefTemplateUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

class AssetsIndexTemplateBuilder {

    private final Logger logger = Logger.getInstance(AssetsIndexTemplateBuilder.class);

    private static final String INDEX_TEMPLATE_NAME = "assetstemplate.ftl";
    private static final String ASSET_SEARCH_ENV_NAME = "assetsSearch";

    private static final String ENV_VARIABLE_IDE = "ide";
    private static final String USER_EMAIL_VARIABLE = "userEmail";
    private static final String IS_OBSERVABILITY_ENABLED_VARIABLE = "isObservabilityEnabled";
    private static final String IS_DOCKER_INSTALLED = "isDockerInstalled";
    private static final String IS_DOCKER_COMPOSE_INSTALLED = "isDockerComposeInstalled";
    private static final String IS_DIGMA_ENGINE_INSTALLED = "isDigmaEngineInstalled";
    private static final String IS_DIGMA_ENGINE_RUNNING = "isDigmaEngineRunning";
    private static final String IS_JAEGER_ENABLED = "isJaegerEnabled";
    private static final String ENVIRONMENT = "environment";

    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    public AssetsIndexTemplateBuilder() {
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), AssetsService.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }

    public InputStream build(Project project) {

        try {
            var data = new HashMap<String, Object>();
            JCefTemplateUtils.addCommonEnvVariables(data);

            var curEnv = PersistenceService.getInstance().getState().getCurrentEnv();
            var assetSearchEnabledForLinux = VersionComparatorUtil.compare(ApplicationInfo.getInstance().getMajorVersion(),"2023") >= 0;
            data.put(ASSET_SEARCH_ENV_NAME, SystemInfo.isLinux ? String.valueOf(assetSearchEnabledForLinux) : "true");

            data.put(ENV_VARIABLE_IDE, ApplicationNamesInfo.getInstance().getProductName());
            data.put(IS_JAEGER_ENABLED, JaegerUtilKt.isJaegerButtonEnabled());
            var userEmail = PersistenceService.getInstance().getState().getUserEmail();
            data.put(USER_EMAIL_VARIABLE, userEmail == null ? "" : userEmail);
            data.put(IS_OBSERVABILITY_ENABLED_VARIABLE, PersistenceService.getInstance().getState().isAutoOtel());
            data.put(IS_DIGMA_ENGINE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isEngineInstalled());
            data.put(IS_DIGMA_ENGINE_RUNNING, ApplicationManager.getApplication().getService(DockerService.class).isEngineRunning(project));
            data.put(IS_DOCKER_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());
            data.put(IS_DOCKER_COMPOSE_INSTALLED, ApplicationManager.getApplication().getService(DockerService.class).isDockerInstalled());
            data.put(ENVIRONMENT, curEnv == null ? "" : curEnv);

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
