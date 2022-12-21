package org.digma.intellij.plugin.debugger

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSessionListener
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log

class SessionListener(private val project: Project) : XDebugSessionListener {

    private val logger: Logger = Logger.getInstance(SessionListener::class.java)

    private val analyticsService = project.getService(AnalyticsService::class.java)

    private var paused = false

    fun isPaused():Boolean{
        return paused
    }

    override fun sessionPaused() {
        paused = true
        val timestamp = System.currentTimeMillis().toString()
        object : Task.Backgroundable(project, "Digma:Send Debugger Session Paused") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    analyticsService.sendDebuggerEvent(0,timestamp)
                } catch (e: Exception) {
                    Log.log(logger::debug, "exception calling sendDebuggerEvent {}. ", e.message)
                }
            }
        }.queue()
    }

    override fun sessionResumed() {
        paused = false
        val timestamp = System.currentTimeMillis().toString()
        object : Task.Backgroundable(project, "Digma:Send Debugger Session Resumed") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    analyticsService.sendDebuggerEvent(1,timestamp)
                } catch (e: Exception) {
                    Log.log(logger::debug, "exception calling sendDebuggerEvent {}. ", e.message)
                }
            }
        }.queue()
    }

}