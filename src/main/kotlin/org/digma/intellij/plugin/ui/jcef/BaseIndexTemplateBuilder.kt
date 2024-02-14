package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.jcef.common.JCefTemplateUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.list.insights.getJaegerUrl
import org.digma.intellij.plugin.ui.list.insights.isJaegerButtonEnabled
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets


private const val ENV_VARIABLE_IDE = "ide"
private const val IS_JAEGER_ENABLED = "isJaegerEnabled"
private const val USER_EMAIL_VARIABLE = "userEmail"
private const val USER_REGISTRATION_EMAIL_VARIABLE = "userRegistrationEmail"
private const val IS_OBSERVABILITY_ENABLED_VARIABLE = "isObservabilityEnabled"
private const val IS_DIGMA_ENGINE_INSTALLED = "isDigmaEngineInstalled"
private const val IS_DIGMA_ENGINE_RUNNING = "isDigmaEngineRunning"
private const val IS_DOCKER_INSTALLED = "isDockerInstalled"
private const val IS_DOCKER_COMPOSE_INSTALLED = "isDockerComposeInstalled"
private const val DIGMA_API_URL = "digmaApiUrl"
private const val JAEGER_URL = "jaegerURL"
private const val IS_MICROMETER_PROJECT = "isMicrometerProject"
private const val ENVIRONMENT = "environment"


abstract class BaseIndexTemplateBuilder(resourceFolderName: String, private val indexTemplateName: String) {

    private val logger = Logger.getInstance(this::class.java)

    private val freemarkerConfiguration = Configuration(Configuration.VERSION_2_3_30)

    init {
        freemarkerConfiguration.setClassForTemplateLoading(this.javaClass, resourceFolderName)
        freemarkerConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name())
        freemarkerConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
        freemarkerConfiguration.setNumberFormat("computer")
    }

    fun build(project: Project): InputStream? {

        return try {

            val data = HashMap<String, Any>()

            //todo: move JCefTemplateUtils.addCommonEnvVariables to here when all apps are using this base class
            JCefTemplateUtils.addCommonEnvVariables(data)
            data[ENV_VARIABLE_IDE] = ApplicationNamesInfo.getInstance().productName
            data[IS_JAEGER_ENABLED] = isJaegerButtonEnabled()
            data[USER_EMAIL_VARIABLE] = PersistenceService.getInstance().getUserEmail() ?: ""
            data[USER_REGISTRATION_EMAIL_VARIABLE] = PersistenceService.getInstance().getUserRegistrationEmail() ?: ""
            data[IS_OBSERVABILITY_ENABLED_VARIABLE] = PersistenceService.getInstance().isAutoOtel()
            data[IS_DIGMA_ENGINE_INSTALLED] = service<DockerService>().isEngineInstalled()
            data[IS_DIGMA_ENGINE_RUNNING] = service<DockerService>().isEngineRunning(project)
            data[IS_DOCKER_INSTALLED] = service<DockerService>().isDockerInstalled()
            data[IS_DOCKER_COMPOSE_INSTALLED] = service<DockerService>().isDockerInstalled()
            data[DIGMA_API_URL] = SettingsState.getInstance().apiUrl
            data[JAEGER_URL] = getJaegerUrl() ?: ""
            data[IS_MICROMETER_PROJECT] = SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer()
            data[ENVIRONMENT] = PersistenceService.getInstance().getCurrentEnv() ?: ""

            addAppSpecificEnvVariable(project, data)

            val template: Template = freemarkerConfiguration.getTemplate(indexTemplateName)
            val stringWriter = StringWriter()
            template.process(data, stringWriter)
            ByteArrayInputStream(stringWriter.toString().toByteArray(StandardCharsets.UTF_8))

        } catch (e: Exception) {
            Log.debugWithException(logger, e, "error creating template for index.html")
            ErrorReporter.getInstance().reportError("BaseIndexTemplateBuilder.build", e)
            null
        }
    }

    /**
     * derived classes can implement to add more environment variables
     */
    open fun addAppSpecificEnvVariable(project: Project, data: java.util.HashMap<String, Any>) {}


}