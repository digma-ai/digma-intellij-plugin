package org.digma.intellij.plugin.debugger

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSessionListener
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log

class SessionListener(project: Project) : XDebugSessionListener {

    private val logger: Logger = Logger.getInstance(SessionListener::class.java)

    private val analyticsService = project.getService(AnalyticsService::class.java)

    private var paused = false

    fun isPaused():Boolean{
        return paused
    }

    override fun sessionPaused() {
        paused = true
        try {
            analyticsService.sendDebuggerEvent(0)
        }catch (e:Exception){
            Log.log(logger::debug, "exception calling sendDebuggerEvent {}. ", e.message)
        }
    }

    override fun sessionResumed() {
        paused = false
        try {
            analyticsService.sendDebuggerEvent(1)
        }catch (e:Exception){
            Log.log(logger::debug, "exception calling sendDebuggerEvent {}. ", e.message)
        }
    }

}