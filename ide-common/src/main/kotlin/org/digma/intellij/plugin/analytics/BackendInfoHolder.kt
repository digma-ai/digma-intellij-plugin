package org.digma.intellij.plugin.analytics

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.minutes

/**
 * keep the backend info and tracks it on connection events.
 * Its necessary because there is code that runs on EDT that may need the backend info. it's possible
 * in that case to do it on background but then the EDT will wait for the api call, and we don't want that.
 */
@Service(Service.Level.PROJECT)
class BackendInfoHolder(val project: Project) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(BackendInfoHolder::class.java)

    private var aboutRef: AtomicReference<AboutResult?> = AtomicReference(null)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): BackendInfoHolder {
            return project.service<BackendInfoHolder>()
        }
    }


    init {

        //update now so that about exists as part of the object instantiation
        updateAboutInBackgroundNowWithTimeout()

        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            while (isActive) {
                update()
                delay(1.minutes.inWholeMilliseconds)
            }
        }

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


    private fun updateInBackground() {
        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            update()
        }
    }


    //must be called in background coroutine
    private fun update() {
        try {
            Log.log(logger::trace, "updating backend info")
            aboutRef.set(AnalyticsService.getInstance(project).about)
            aboutRef.get()?.let {
                ActivityMonitor.getInstance(project).registerServerInfo(it)
            }
            Log.log(logger::trace, "backend info updated {}", aboutRef.get())
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error in update")
            val isConnectionException = ExceptionUtils.isAnyConnectionException(e)
            if (!isConnectionException) {
                ErrorReporter.getInstance().reportError(project, "BackendInfoHolder.update", e)
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
        updateAboutInBackgroundNowWithTimeout()
        return aboutRef.get()
    }


    private fun updateAboutInBackgroundNowWithTimeout() {

        Log.log(logger::trace, "updating backend info in background with timeout")

        @Suppress("UnstableApiUsage")
        val deferred = disposingScope().async {
            update()
        }

        runBlocking {
            try {
                withTimeout(2000) {
                    deferred.await()
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error in updateAboutInBackgroundNowWithTimeout")
                ErrorReporter.getInstance().reportError("BackendInfoHolder.updateAboutInBackgroundNowWithTimeout", e)
            }
        }

        Log.log(logger::trace, "backend info updated in background with timeout {}", aboutRef.get())
    }


}