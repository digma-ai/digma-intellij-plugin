package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import org.digma.intellij.plugin.digmathon.DigmathonService
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.env.Env
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.common.getJaegerUrl
import org.digma.intellij.plugin.ui.common.isJaegerButtonEnabled
import org.digma.intellij.plugin.ui.settings.Theme
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import kotlin.collections.set


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
private const val DIGMA_API_PROXY_PREFIX = "digmaApiProxyPrefix"
private const val JAEGER_URL = "jaegerURL"
private const val IS_MICROMETER_PROJECT = "isMicrometerProject"
private const val ENVIRONMENT = "environment"
private const val ENV_VARIABLE_THEME = "theme"
private const val ENV_VARIABLE_FONT = "mainFont"
private const val ENV_VARIABLE_CODE_FONT = "codeFont"
const val DIGMATHON_ENABLED = "isDigmathonModeEnabled"
const val DIGMATHON_PRODUCT_KEY = "productKey"


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

            val data = mutableMapOf<String, Any>()

            addCommonEnvVariables(data)
            data[ENV_VARIABLE_IDE] = ApplicationNamesInfo.getInstance().productName
            data[IS_JAEGER_ENABLED] = isJaegerButtonEnabled()
            data[USER_EMAIL_VARIABLE] = PersistenceService.getInstance().getUserEmail() ?: ""
            data[USER_REGISTRATION_EMAIL_VARIABLE] = PersistenceService.getInstance().getUserRegistrationEmail() ?: ""
            data[IS_OBSERVABILITY_ENABLED_VARIABLE] = PersistenceService.getInstance().isObservabilityEnabled()
            data[IS_DIGMA_ENGINE_INSTALLED] = service<DockerService>().isEngineInstalled()
            data[IS_DIGMA_ENGINE_RUNNING] = service<DockerService>().isEngineRunning(project)
            data[IS_DOCKER_INSTALLED] = service<DockerService>().isDockerInstalled()
            data[IS_DOCKER_COMPOSE_INSTALLED] = service<DockerService>().isDockerInstalled()
            data[DIGMA_API_URL] = SettingsState.getInstance().apiUrl
            data[DIGMA_API_PROXY_PREFIX] = ApiProxyResourceHandler.URL_PREFIX
            data[JAEGER_URL] = getJaegerUrl() ?: ""
            data[IS_MICROMETER_PROJECT] = SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer()
            data[ENVIRONMENT] = Env.getCurrentEnv(project)?.let { it: Env -> serializeObjectToJson(it) } ?: "undefined"
            data[DIGMATHON_ENABLED] = DigmathonService.getInstance().getDigmathonState().isActive()
            data[DIGMATHON_PRODUCT_KEY] = DigmathonService.getInstance().getProductKey().orEmpty()

            addAppSpecificEnvVariable(project, data)

            val template: Template = freemarkerConfiguration.getTemplate(indexTemplateName)
            val stringWriter = StringWriter()
            template.process(data, stringWriter)
            ByteArrayInputStream(stringWriter.toString().toByteArray(StandardCharsets.UTF_8))

        } catch (e: Exception) {
            Log.debugWithException(logger, e, "error creating template for index.html")
            ErrorReporter.getInstance().reportError(project, "BaseIndexTemplateBuilder.build", e)
            null
        }
    }

    /**
     * derived classes can implement to add more environment variables
     */
    open fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {}


}


fun addCommonEnvVariables(env: MutableMap<String, Any>) {
    env[ENV_VARIABLE_THEME] = if (JBColor.isBright()) Theme.LIGHT.themeName else Theme.DARK.themeName
    env[ENV_VARIABLE_FONT] = UIUtil.getLabelFont().fontName
    env[ENV_VARIABLE_CODE_FONT] = AppEditorFontOptions.getInstance().fontPreferences.fontFamily
}