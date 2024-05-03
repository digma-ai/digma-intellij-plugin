package org.digma.intellij.plugin.digmathon

import com.fasterxml.jackson.core.type.TypeReference
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
import org.digma.intellij.plugin.common.createObjectMapperWithJavaTimeModule
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlin.time.Duration.Companion.minutes


@Service(Service.Level.APP)
class DigmathonService : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val digmathonInfo = AtomicReference(
        DigmathonInfo(
            LocalDate.of(2024, 5, 1).atStartOfDay().atZone(ZoneId.systemDefault()),
            LocalDate.of(2024, 5, 14).atStartOfDay().atZone(ZoneId.systemDefault())
        )
    )

    val viewedInsights: MutableMap<String, Instant> = readInsightsViewedFromPersistence()

    var isUserFinishedDigmathon = PersistenceService.getInstance().isFinishDigmathonGameForUser()


    companion object {

        private val objectMapper = createObjectMapperWithJavaTimeModule()

        @JvmStatic
        fun getInstance(): DigmathonService {
            return service<DigmathonService>()
        }
    }


    override fun dispose() {
        //do nothing, used as parent disposable
    }


    init {

        if (isDigmathonStartedForUser() && digmathonInfo.get().isEnded()) {
            end()
        } else {

            @Suppress("UnstableApiUsage")
            disposingScope().launch {

                //let the project load and hopefully all jcef apps
                delay(1.minutes.inWholeMilliseconds)

                while (isActive && digmathonInfo.get().isActive()) {
                    try {

                        if (!isDigmathonStartedForUser() && digmathonInfo.get().isActive()) {
                            start()
                        }

                        if (isDigmathonStartedForUser() && digmathonInfo.get().isEnded()) {
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

                //the job may be canceled by the system, do a last check
                if (isDigmathonStartedForUser() && digmathonInfo.get().isEnded()) {
                    end()
                }
            }
        }
    }


    //this is needed so that we can call start and end only once
    private fun isDigmathonStartedForUser(): Boolean {
        return PersistenceService.getInstance().isDigmathonStartedForUser()
    }

    private fun markStartedDigmathonForUser() {
        PersistenceService.getInstance().setDigmathonStartedForUser(true)
    }

    private fun markEndedDigmathonForUser() {
        PersistenceService.getInstance().setDigmathonStartedForUser(false)
    }


    private fun start() {
        //reset persistence properties every time a new digmathon starts
        PersistenceService.getInstance().setFinishDigmathonGameForUser(false)
        PersistenceService.getInstance().setDigmathonInsightsViewed(null)
        PersistenceService.getInstance().setDigmathonInsightsViewedLastUpdated(null)
        markStartedDigmathonForUser()
        fireStateChangedEvent()
        reportEvent("start")
    }


    private fun end() {
        //reset persistence properties for next time
        PersistenceService.getInstance().setFinishDigmathonGameForUser(false)
        PersistenceService.getInstance().setDigmathonInsightsViewed(null)
        PersistenceService.getInstance().setDigmathonInsightsViewedLastUpdated(null)
        markEndedDigmathonForUser()
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
                NotificationUtil.showBalloonWarning(
                    it,
                    "Invalid Product Key",
                    "You can reopen the onboarding menu from the main panel menu to try again"
                )
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


    private fun reportEvent(eventType: String, details: Map<String, String> = mapOf()) {
        findActiveProject()?.let {
            ActivityMonitor.getInstance(it).reportDigmathonEvent(eventType, details)
        }
    }

    fun addInsightsViewed(insightsTypesViewed: List<String>) {
        if (digmathonInfo.get().isActive() && !allInsightsExists(insightsTypesViewed)) {
            //add only new insight types
            insightsTypesViewed.forEach {
                this.viewedInsights.computeIfAbsent(it) { Instant.now() }
            }
            flushInsightsViewedToPersistence()
        }
    }

    private fun allInsightsExists(insightsTypesViewed: List<String>): Boolean {
        return this.viewedInsights.keys.containsAll(insightsTypesViewed)
    }


    private fun flushInsightsViewedToPersistence() {
        try {
            val json = objectMapper.writeValueAsString(viewedInsights)
            PersistenceService.getInstance().setDigmathonInsightsViewed(json)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("DigmathonService.flushInsightsViewedToPersistence", e)
        }
    }

    private fun readInsightsViewedFromPersistence(): MutableMap<String, Instant> {
        val json = PersistenceService.getInstance().getDigmathonInsightsViewed() ?: return mutableMapOf()
        return try {
            val ref = object : TypeReference<Map<String, Instant>>() {}
            val viewedInsights = objectMapper.readValue(json, ref)
            viewedInsights.toMutableMap()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("DigmathonService.readInsightsViewedFromPersistence", e)
            mutableMapOf()
        }
    }


    fun getDigmathonInsightsViewedLastUpdated(): Instant? {
        return PersistenceService.getInstance().getDigmathonInsightsViewedLastUpdated()
    }

    fun updateDigmathonInsightsViewedLastUpdated() {
        PersistenceService.getInstance().setDigmathonInsightsViewedLastUpdated(Instant.now())
    }

    fun setFinishDigmathonGameForUser() {
        PersistenceService.getInstance().setFinishDigmathonGameForUser(true)
        isUserFinishedDigmathon = true
        fireUserFinishedDigmathon()
        reportEvent("user finished digmathon")
    }


    private fun fireUserFinishedDigmathon() {
        ApplicationManager.getApplication().messageBus.syncPublisher(UserFinishedDigmathonEvent.USER_FINISHED_DIGMATHON_TOPIC)
            .userFinishedDigmathon()
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


}