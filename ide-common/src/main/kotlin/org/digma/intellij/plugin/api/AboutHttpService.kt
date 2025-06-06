package org.digma.intellij.plugin.api

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import com.intellij.util.PlatformUtils
import com.intellij.util.io.jackson.array
import com.intellij.util.io.jackson.obj
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.digma.intellij.plugin.analytics.getBackendDeploymentType
import org.digma.intellij.plugin.analytics.getBackendVersion
import org.digma.intellij.plugin.analytics.isCentralized
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import java.io.OutputStream

/**
 * {get} /digma/about The application info
 */
internal class AboutHttpService : AbstractHttpService() {

    override fun getServiceName() = "digma/about"


    override suspend fun executeRequest(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
        val byteOut = BufferExposingByteArrayOutputStream()
        writeApplicationInfoJson(byteOut)
        send(byteOut, request, context)
        return null
    }


    private fun writeApplicationInfoJson(out: OutputStream) {
        JsonFactory().createGenerator(out).useDefaultPrettyPrinter().use { writer ->
            writer.obj {
                writeAboutJson(writer)
            }
        }
    }


    private fun writeAboutJson(writer: JsonGenerator) {
        writer.writeStringField("source", "Digma Plugin")
        var appName = ApplicationInfo.getInstance().fullApplicationName
        @Suppress("UnstableApiUsage")
        if (!PlatformUtils.isIdeaUltimate()) {
            val productName = ApplicationNamesInfo.getInstance().productName
            appName = appName
                .replace("$productName ($productName)", productName)
                .removePrefix("JetBrains ")
        }
        writer.writeStringField("name", appName)
        writer.writeStringField("productName", ApplicationNamesInfo.getInstance().productName)
        writer.writeStringField("edition", ApplicationNamesInfo.getInstance().editionName)

        val build = ApplicationInfo.getInstance().build
        writer.writeNumberField("baselineVersion", build.baselineVersion)
        if (!build.isSnapshot) {
            writer.writeStringField("buildNumber", build.asStringWithoutProductCode())
        }

        writer.writeStringField("pluginVersion", SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown"))
        writer.writeStringField("backendVersion", getBackendVersion())
        writer.writeStringField("backendDeploymentType", getBackendDeploymentType())
        writer.writeBooleanField("isCentralized", isCentralized())

        writer.array("openProjects") {
            for (project in ProjectManager.getInstance().openProjects) {
                writer.writeString(project.name)
            }
        }
    }

}

