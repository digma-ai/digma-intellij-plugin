package org.digma.intellij.plugin.analytics

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import java.util.concurrent.atomic.AtomicReference

/**
 * keep the backend info and tracks it on connection events.
 * Its necessary because there is code that runs on EDT that may need the backend info. it's possible
 * in that case to do it on background but then the EDT will wait for the api call, and we don't want that.
 */
@Service(Service.Level.PROJECT)
class BackendInfoHolder(val project: Project) : Disposable {

    private val logger: Logger = Logger.getInstance(BackendInfoHolder::class.java)

    private var aboutRef: AtomicReference<AboutResult?> = AtomicReference(null)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): BackendInfoHolder {
            return project.service<BackendInfoHolder>()
        }
    }

    fun loadOnStartup() {
        updateInBackground()
    }


    override fun dispose() {
        //nothing to do, used as parent disposable
    }

    init {
        project.messageBus.connect(this)
            .subscribe(
                AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
                object : AnalyticsServiceConnectionEvent {
                    override fun connectionLost() {
                        Log.log(logger::debug, "got connectionLost")
                    }

                    override fun connectionGained() {
                        Log.log(logger::debug, "got connectionGained")
                        updateInBackground()
                    }
                })

        project.messageBus.connect(this)
            .subscribe(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC, ApiClientChangedEvent {
                Log.log(logger::debug, "got apiClientChanged")
                updateInBackground()
            })
    }


    //updateInBackground is also called every time the analytics client is replaced
    private fun updateInBackground() {
        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            try {

                aboutRef.set(AnalyticsService.getInstance(project).about)

            } catch (e: Throwable) {
                val isConnectionException = ExceptionUtils.isAnyConnectionException(e)

                if (!isConnectionException) {
                    ErrorReporter.getInstance().reportError("BackendUtilsKt.updateInBackground", e)
                }
            }
        }
    }



    fun getAbout(): AboutResult? {
        if (aboutRef.get() == null) {
            return getAboutInBackgroundNow()
        }

        return aboutRef.get()
    }


    private fun getAboutInBackgroundNow(): AboutResult? {
        if (aboutRef.get() == null) {
            return getAboutInBackgroundNowWithTimeout()
        }
        return aboutRef.get()
    }



    fun isCentralized(): Boolean {
        return aboutRef.get()?.let {
            it.isCentralize ?: false
        } ?: getIsCentralizedInBackgroundNow()
    }


    private fun getIsCentralizedInBackgroundNow(): Boolean {
        return getAboutInBackgroundNowWithTimeout()?.isCentralize ?: false
    }


    private fun getAboutInBackgroundNowWithTimeout(): AboutResult? {

        @Suppress("UnstableApiUsage")
        val deferred = disposingScope().async {
            try {
                aboutRef.set(AnalyticsService.getInstance(project).about)
            } catch (e: Throwable) {
                val isConnectionException = ExceptionUtils.isAnyConnectionException(e)
                if (!isConnectionException) {
                    ErrorReporter.getInstance().reportError("BackendUtilsKt.getAboutInBackgroundNowWithTimeout", e)
                }
            }
        }


        return runBlocking {
            try {
                withTimeout(5000) {
                    deferred.await()
                }
                aboutRef.get()
            } catch (e: Throwable) {
                null
            }
        }
    }

}