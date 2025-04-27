package org.digma.intellij.plugin.ui.jcef.pluginapi

import com.intellij.openapi.project.Project
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.reflect.full.createInstance

const val CommandQueryName = "pluginCommand"

abstract class Command {

    enum class Commands(val commandName: String, val className: String) {
        RECENT_ACTIVITY_BADGE("RecentActivityBadge","org.digma.intellij.plugin.ui.jcef.pluginapi.RecentActivityBadgeCommand");


        companion object {
            private val NAME_MAP = values().associateBy { it.commandName.lowercase() }

            fun fromCommandName(commandName: String): Commands? {
                return NAME_MAP[commandName.lowercase()]
            }
        }
    }


    companion object{

        fun getCommand(apiUrl: URL): Command? {

            val query = apiUrl.query ?: return null // get query string or return null if none

            val commandName = query.split("&")
                .mapNotNull { it.split("=", limit = 2).takeIf { it.size == 2 } }
                .firstOrNull { it[0] == CommandQueryName }
                ?.let { URLDecoder.decode(it[1], StandardCharsets.UTF_8) }

            if (commandName == null) {
                return null
            }

            val commandEnum = Commands.fromCommandName(commandName) ?: return null
            val commandClassName = commandEnum.className
            val commandClass = Class.forName(commandClassName)
            val instance = commandClass.kotlin.createInstance()

            return instance as? Command
        }
    }

    abstract fun execute(project: Project, requestId: Long, requestMethod: String, postData: ByteArray?, headers: Map<String, String>): PluginApiHttpResponse

}