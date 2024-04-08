package org.digma.intellij.plugin.digmathon

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlin.time.Duration.Companion.minutes


@Service(Service.Level.APP)
class DigmathonService : Disposable {


    private val logger = Logger.getInstance(this::class.java)

    private val digmathonInfo = AtomicReference(
        DigmathonInfo(
            LocalDate.of(2024, 4, 5).atStartOfDay().atZone(ZoneId.systemDefault()),
            LocalDate.of(2024, 4, 13).atStartOfDay().atZone(ZoneId.systemDefault())
        )
    )

    private val isDigmathonActive = AtomicBoolean(digmathonInfo.get().isActive())

    val viewedInsights = PersistenceService.getInstance().getDigmathonInsightsViewed()
        ?.split(",")?.toMutableSet() ?: mutableSetOf()

    var isUserFinishedDigmathon = PersistenceService.getInstance().isFinishDigmathonGameForUser()


    companion object {
        @JvmStatic
        fun getInstance(): DigmathonService {
            return service<DigmathonService>()
        }
    }


    override fun dispose() {
        //do nothing, used as parent disposable
    }


    init {

        //for development,simulate a 5 minutes event that starts 2 minutes after IDE start and lasts for 5 minutes
        val simulateStart = System.getProperty("org.digma.digmathon.simulate.startAfterMinutes")
        val simulatePeriod = System.getProperty("org.digma.digmathon.simulate.periodMinutes")

        if (simulateStart != null) {
            val startAfter = simulateStart.toLong()
            val endAfter = (simulatePeriod.toLongOrNull() ?: 10) + startAfter

            digmathonInfo.set(
                DigmathonInfo(
                    LocalDateTime.now().plusMinutes(startAfter).atZone(ZoneId.systemDefault()),
                    LocalDateTime.now().plusMinutes(endAfter).atZone(ZoneId.systemDefault())
                )
            )
            DigmathonProductKey().clear()
            isDigmathonActive.set(digmathonInfo.get().isActive())
            isUserFinishedDigmathon = false
        }


        if (isDigmathonActive.get() && digmathonInfo.get().isEnded()) {
            end()
        } else {

            @Suppress("UnstableApiUsage")
            disposingScope().launch {

                //let the project load and hopefully all jcef apps
                delay(1.minutes.inWholeMilliseconds)

                while (isActive) {
                    try {

                        if (!isDigmathonActive.get() && digmathonInfo.get().isActive()) {
                            start()
                        }

                        if (isDigmathonActive.get() && digmathonInfo.get().isEnded()) {
                            end()
                            cancel("digmathon ended")
                        }

                        delay(1.minutes.inWholeMilliseconds)

                    } catch (ce: CancellationException) {
                        Log.log(logger::info, "digmathon timer canceled {}", ce)
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, e, "error in digmathon timer {}", e)
                        ErrorReporter.getInstance().reportError("DigmathonService.timer", e)
                    }
                }
            }
        }
    }


    private fun start() {
        isDigmathonActive.set(true)
        fireStateChangedEvent()
        reportEvent("start")
    }


    private fun end() {
        isDigmathonActive.set(false)
        DigmathonProductKey().clear()
        fireStateChangedEvent()
        fireProductKeyStateChanged()
        reportEvent("end")
    }


    //user is active only with these conditions
    fun isUserActive(): Boolean {
        return digmathonInfo.get().isActive() && getProductKey() != null
    }

    private fun fireStateChangedEvent() {
        ApplicationManager.getApplication().messageBus.syncPublisher(DigmathonActivationEvent.DIGMATHON_ACTIVATION_TOPIC)
            .digmathonActivationStateChanged(digmathonInfo.get().isActive())
    }


    fun getDigmathonState(): DigmathonInfo {
        return digmathonInfo.get()
    }

    fun setProductKey(productKey: String) {
        try {
            allowSlowOperation {
                DigmathonProductKey().validateAndSave(productKey)
                reportEvent("set product key")
            }
        } catch (e: InvalidProductKeyException) {
            reportEvent("product key invalid", mapOf("productKey" to e.productKey))
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.setProductKey", e)
            findActiveProject()?.let {
                NotificationUtil.showBalloonWarning(it, "invalid Digmathon product key")
            }

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.setProductKey", e)
        } finally {
            fireProductKeyStateChanged()
        }
    }

    private fun fireProductKeyStateChanged() {
        ApplicationManager.getApplication().messageBus.syncPublisher(DigmathonProductKeyStateChangedEvent.PRODUCT_KEY_STATE_CHANGED_TOPIC)
            .productKey(getProductKey())
    }


    fun getProductKey(): String? {
        return try {
            allowSlowOperation(Supplier {
                DigmathonProductKey().validateAndGet()
            })
        } catch (e: InvalidProductKeyException) {
            reportEvent("product key invalid", mapOf("productKey" to e.productKey))
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.setProductKey", e)
            null
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.setProductKey", e)
            null
        }
    }


    data class DigmathonInfo(val startTime: ZonedDateTime, val endTime: ZonedDateTime) {

        fun isActive(): Boolean {
            val now = LocalDateTime.now().atZone(ZoneId.systemDefault())

            return (now.equals(startTime) || now.isAfter(startTime)) &&
                    now.isBefore(endTime)
        }

        fun isEnded(): Boolean {
            return !isActive()
        }
    }


    private fun reportEvent(eventType: String, details: Map<String, String> = mapOf()) {
        findActiveProject()?.let {
            ActivityMonitor.getInstance(it).reportDigmathonEvent(eventType, details)
        }
    }

    fun addInsightsViewed(insightsTypesViewed: List<String>) {
        if (digmathonInfo.get().isActive()) {
            this.viewedInsights.addAll(insightsTypesViewed)
            flushInsightsViewedToPersistence()
        }
    }

    private fun flushInsightsViewedToPersistence() {
        PersistenceService.getInstance().setDigmathonInsightsViewed(viewedInsights.joinToString(","))
    }

    fun setFinishDigmathonGameForUser() {
        PersistenceService.getInstance().setFinishDigmathonGameForUser()
        isUserFinishedDigmathon = true
        fireUserFinishedDigmathon()
        reportEvent("user finished game")
    }


    private fun fireUserFinishedDigmathon() {
        ApplicationManager.getApplication().messageBus.syncPublisher(UserFinishedDigmathonEvent.USER_FINISHED_DIGMATHON_TOPIC)
            .userFinishedDigmathon()
    }

}