package org.digma.intellij.plugin.analytics

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * keep the backend info and tracks it on connection events.
 * Its necessary because there is code that runs on EDT that may need the backend info. it's possible
 * in that case to do it on background but then the EDT will wait for the api call, and we don't want that.
 */
@Service(Service.Level.APP)
class BackendInfoHolder : Disposable {

    private val logger: Logger = Logger.getInstance(BackendInfoHolder::class.java)

    private var aboutRef: AtomicReference<AboutResult?> = AtomicReference(null)

    companion object {
        @JvmStatic
        fun getInstance(): BackendInfoHolder {
            return service<BackendInfoHolder>()
        }
    }

    override fun dispose() {
        //nothing to do, used as parent disposable
    }

    init {
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(BackendConnectionEvent.BACKEND_CONNECTION_STATE_TOPIC, object : BackendConnectionEvent {
                override fun connectionLost() {
                    Log.log(logger::debug, "got connectionLost")
                }

                override fun connectionGained() {
                    Log.log(logger::debug, "got connectionGained")
                    updateInBackground()
                }
            })
    }


    fun getAbout(): AboutResult? {
        return aboutRef.get()
    }

    //updateInBackground is also called every time the analytics client is replaced
    fun updateInBackground() {
        Backgroundable.ensurePooledThreadWithoutReadAccess {
            findActiveProject()?.let {
                aboutRef.set(AnalyticsService.getInstance(it).about)
            }
        }
    }

    fun isCentralized(): Boolean {
        return aboutRef.get()?.let {
            it.isCentralize ?: false
        } ?: findActiveProject()?.let {
            isCentralized(it)
        } ?: false
    }

    fun isCentralized(project: Project): Boolean {
        return aboutRef.get()?.let {
            it.isCentralize ?: false
        } ?: getInBackgroundNow(project)
    }

    private fun getInBackgroundNow(project: Project): Boolean {

        return try {

            val future = Backgroundable.ensurePooledThreadWithoutReadAccess(Callable {
                AnalyticsService.getInstance(project).about
            })

            aboutRef.set(future.get(5, TimeUnit.SECONDS))
            aboutRef.get()?.isCentralize ?: false

        } catch (e: Throwable) {
            val isConnectionException = ExceptionUtils.isAnyConnectionException(e)

            if (!isConnectionException) {
                ErrorReporter.getInstance().reportError("BackendUtilsKt.isCentralized", e)
            }

            false
        }
    }
}