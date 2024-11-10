package org.digma.intellij.plugin.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import io.netty.handler.codec.http.QueryStringDecoder
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.log.Log
import org.jetbrains.ide.RestService.Companion.getStringParameter

@Service(Service.Level.APP)
class ApiService : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(this::class.java)

    fun executeAction(action: String, urlDecoder: QueryStringDecoder) {

        val projectName = getStringParameter(PROJECT_NAME_PARAM_NAME, urlDecoder)
        val project = projectName?.let {
            findProjectOrNull(projectName)
        } ?: findActiveOrRecentProject()

        if (project == null) {
            Log.log(logger::warn, "could not find a project to use for api call")
            throw NoProjectException("could not find project $projectName nor any other project to use")
        }

        Log.log(logger::trace,"found project to use {}",project.name)

        project.service<ApiProjectService>().executeAction(action, urlDecoder)
    }

}