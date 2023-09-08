package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.collaboration.async.DisposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.recentactivity.model.LiveDataMessage
import org.digma.intellij.plugin.ui.recentactivity.model.LiveDataPayload
import java.util.concurrent.atomic.AtomicBoolean


private const val RECENT_ACTIVITY_SET_LIVE_DATA = "RECENT_ACTIVITY/SET_LIVE_DATA"


@Service(Service.Level.PROJECT)
class LiveViewUpdater(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    private var myDisposable: Disposable? = null

    private var myJob: Job? = null

    private val jcefComponentIsNull: AtomicBoolean = AtomicBoolean(true)

    override fun dispose() {
        myDisposable?.dispose()
    }


    fun setJcefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
        jcefComponentIsNull.set(false)
    }


    @Synchronized
    fun sendLiveData(codeObjectId: String) {

        Log.log(logger::trace, project, "Got sendLiveData request for {}", codeObjectId)

        myDisposable?.dispose()

        myDisposable = Disposer.newDisposable()

        myJob?.cancel()

        @Suppress("UnstableApiUsage")
        myJob = DisposingScope(myDisposable!!).launch {

            Log.log(logger::trace, project, "live view timer started for {}", codeObjectId)

            //jcefComponentIsNull when clicking live view when the tool window was not initialized yet,
            // wait for it, it takes just milliseconds
            while (jcefComponentIsNull.get() && isActive) {
                delay(5)
            }

            Log.log(logger::trace, project, "live view timer running after jcefComponent is not null for {}", codeObjectId)

            while (isActive) {
                try {
                    val durationData = AnalyticsService.getInstance(project).getDurationLiveData(codeObjectId)
                    Log.log(logger::trace, project, "live view timer got live data for {},{}", codeObjectId, durationData)
                    if (!isActive) {
                        break
                    }
                    sendLiveData(durationData)
                    if (!isActive) {
                        break
                    }
                    delay(5000)
                } catch (e: CancellationException) {
                    Log.log(logger::trace, project, "live view timer job canceled for {}", codeObjectId)
                    break
                } catch (e: Exception) {
                    Log.warnWithException(logger, project, e, "exception in live data timer")
                    ErrorReporter.getInstance().reportError("LiveViewUpdater.timer", e)
                    delay(5000)
                }
            }

        }
    }


    private fun sendLiveData(durationLiveData: DurationLiveData) {

        jCefComponent?.let { jcefComp ->

            //should not happen but we need to check
            if (durationLiveData.durationData == null) {
                Log.log(logger::debug, project, "durationLiveData.getDurationData is null, not sending live data for {}", durationLiveData)
                return
            }

            Log.log(logger::debug, project, "sending live data if not null for {}", durationLiveData)

            durationLiveData.durationData?.let { durationData ->

                Log.log(logger::debug, project, "sending live data for {}", durationData.codeObjectId)
                val liveDataMessage = LiveDataMessage(
                    JCefMessagesUtils.REQUEST_MESSAGE_TYPE, RECENT_ACTIVITY_SET_LIVE_DATA,
                    LiveDataPayload(durationLiveData.liveDataRecords, durationData)
                )

                Log.log(logger::debug, project, "sending LiveDataMessage for {},{}", durationData.codeObjectId, liveDataMessage)
                serializeAndExecuteWindowPostMessageJavaScript(jcefComp.jbCefBrowser.cefBrowser, liveDataMessage)
            }

        }
    }


    @Synchronized
    fun stopLiveView(codeObjectId: String?) {
        //in the future we may have multiple live views, in that case we will recognize them by codeObjectId
        myDisposable?.dispose()
        myJob?.cancel()
    }


}