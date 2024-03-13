package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.createObjectMapper

abstract class BaseCommonMessageRouterHandler(protected val project: Project) : CommonMessageRouterHandler {

    protected val logger = Logger.getInstance(this::class.java)

    override val objectMapper: ObjectMapper = createObjectMapper()
}