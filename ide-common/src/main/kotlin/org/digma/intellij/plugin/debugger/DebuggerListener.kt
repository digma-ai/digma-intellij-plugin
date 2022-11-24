package org.digma.intellij.plugin.debugger

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManagerListener

class DebuggerListener(private val project: Project): XDebuggerManagerListener {

    private var currentSessionListener: SessionListener? = null

    override fun currentSessionChanged(previousSession: XDebugSession?, currentSession: XDebugSession?) {

        //if debugger paused on breakpoint and user kills the debug session without resuming then
        //sessionResumed will not be called. here we track the state of SessionListener,
        //if it was paused but never resumed and now the debug session changed then call sessionResumed.
        //on session start isPaused will return false
        currentSessionListener?.let {
            if (it.isPaused()){
                it.sessionResumed()
            }
        }

        currentSessionListener?.let {
            previousSession?.removeSessionListener(it)
        }


        currentSession?.let {
            currentSessionListener = SessionListener(project)
            it.addSessionListener(currentSessionListener!!)
        }
    }
}