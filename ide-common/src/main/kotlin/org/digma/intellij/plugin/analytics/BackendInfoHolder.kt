package org.digma.intellij.plugin.analytics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.jsonToObject
import org.digma.intellij.plugin.common.objectToJson
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingOneShotDelayedTask
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.scheduling.oneShotTask
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.Throws
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * keep the backend info and tracks it on connection events.
 */
@Service(Service.Level.PROJECT)
class BackendInfoHolder(val project: Project) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(BackendInfoHolder::class.java)

    /**
     * aboutRef should always have an AboutResult object.
     * on startup there are many calls to isCentralized and getAbout. if the AboutResult is null callers will not
     * have the correct info. trying to load the info in background may cause issues and may take few seconds
     * because it may hit initialization of services and that may take too much time. we have experience that this initialization may take
     * few seconds and may cause thread interruptions.
     * the solution to the above is to save the about info as json string in persistence every time it is refreshed. and on startup load it from
     * persistence. when loading from persistence on startup the info may not be up to date, maybe the backend was updated. but the correct info
     * will be populated very soon in the periodic task.
     * if there is no connection or the first periodic task didn't update the ref yet then at least we have info from the last IDE session which in
     * most cases is probably correct.
     * loading from persistence is very fast and we will have info very early on startup to all requesters.
     */
    private var aboutRef: AtomicReference<AboutResult> = AtomicReference(loadAboutInfoFromPersistence())

    companion object {
        @JvmStatic
        fun getInstance(project: Project): BackendInfoHolder {
            return project.service<BackendInfoHolder>()
        }

        private fun saveAboutInfoToPersistence(aboutResult: AboutResult) {
            try {
                val aboutAsJson = objectToJson(aboutResult)
                PersistenceService.getInstance().saveAboutAsJson(aboutAsJson)
            }catch (e:Throwable){
                ErrorReporter.getInstance().reportError("BackendInfoHolder.saveAboutInfoToPersistence",e)
            }
        }

        private fun loadAboutInfoFromPersistence(): AboutResult {
            return try {
                val aboutAsJson = PersistenceService.getInstance().getAboutAsJson()
                aboutAsJson?.let {
                    jsonToObject(it, AboutResult::class.java)
                } ?: AboutResult.UNKNOWN
            }catch (e:Throwable){
                ErrorReporter.getInstance().reportError("BackendInfoHolder.loadAboutInfoFromPersistence",e)
                AboutResult.UNKNOWN
            }
        }

    }


    init {

        //schedule a periodic task that will update the backend info as soon as possible and then again every 1 minute
        val registered = disposingPeriodicTask("BackendInfoHolder.periodic", 1.minutes.inWholeMilliseconds, false) {
            update()
        }

        if (!registered) {
            Log.log(logger::warn, "could not schedule periodic task for BackendInfoHolder")
            ErrorReporter.getInstance().reportError(
                project, "BackendInfoHolder.init", "could not schedule periodic task for BackendInfoHolder",
                mapOf()
            )
        }


        AuthManager.getInstance().addAuthInfoChangeListener({
            Log.log(logger::debug, "got authInfoChanged")
            updateInBackground()
        }, this)


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
        //let it finish without waiting for timeout and without blocking this thread
        oneShotTask("BackendInfoHolder.updateInBackground") {
            update()
        }
    }


    //must be called in background coroutine.
    //if failed update will try again after 5 seconds
    private fun update() {
        try {
            updateImpl()
        } catch (e: Throwable) {
            Log.log(logger::trace, "update failed  trying again in 5 seconds")
            //if update fails run another try after 2 seconds. maybe it was a momentary error from AnalyticsService.
            // if that will not succeed there will be another periodic update soon
            disposingOneShotDelayedTask("BackendInfoHolder.update-fallback", 5.seconds.inWholeMilliseconds) {
                Log.log(logger::trace, "calling updateImpl after 5 seconds delay")
                updateImpl()
            }
        }
    }


    //Note that updateImpl rethrows exceptions
    @Throws(Throwable::class)
    private fun updateImpl(){
        try {
            if (isProjectValid(project)) {
                Log.log(logger::trace, "updating backend info")
                val about = AnalyticsService.getInstance(project).about
                aboutRef.set(about)
                ActivityMonitor.getInstance(project).registerServerInfo(about)
                saveAboutInfoToPersistence(about)
                Log.log(logger::trace, "backend info updated {}", aboutRef.get())
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error in update {}",e)
            val isConnectionException = ExceptionUtils.isAnyConnectionException(e)
            if (!isConnectionException) {
                ErrorReporter.getInstance().reportError(project, "BackendInfoHolder.update", e)
            }
            throw e;
        }
    }





    fun getAbout(): AboutResult {
        return aboutRef.get()
    }


    fun isCentralized(): Boolean {
        return aboutRef.get().isCentralize ?: false
    }


    fun refresh() {
        updateInBackground()
    }
}