package org.digma.intellij.plugin.api

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.QueryStringDecoder

abstract class AbstractApiCommand {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    abstract suspend fun execute(project: Project, urlDecoder: QueryStringDecoder)

}