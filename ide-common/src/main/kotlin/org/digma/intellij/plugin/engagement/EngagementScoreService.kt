package org.digma.intellij.plugin.engagement

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


@Service(Service.Level.APP)
class EngagementScoreService(private val cs: CoroutineScope) : DisposableAdaptor {

    val logger = Logger.getInstance(this::class.java)

    companion object {

        @JvmStatic
        fun getInstance(): EngagementScoreService {
            return service<EngagementScoreService>()
        }

        val MEANINGFUL_ACTIONS = setOf(
            "span link clicked",
            "insights insight card asset link clicked",
            "insights issue card title asset link clicked",
            "highlights top issues card asset link clicked",
            "insights issue card clicked",
            "highlights top issues card table row clicked",
            "trace button clicked",
            "errors trace button clicked",
            "errors error stack trace item clicked",
            "insights jira ticket info button clicked",
            "open histogram",
            "issues filter changed",
        )

        private val PERIOD_TO_TRACK = DatePeriod.parse("P21D")
        private val TIME_ZONE = TimeZone.currentSystemDefault()

        private fun today(): LocalDate {
            return Clock.System.todayIn(TIME_ZONE)
        }
    }


    init {

        disposingPeriodicTask("DailyEngagementScore", 1.minutes.inWholeMilliseconds, 6.hours.inWholeMilliseconds, false) {
            try {
                service<EngagementScorePersistenceService>().removeOldEntries(today(), PERIOD_TO_TRACK)
                if (isDailyEventTime()) {
                    sendEvent()
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("EngagementScoreManager.DailyEngagementScore", e)
            }
        }
    }

    private fun sendEvent() {

        //compute average that includes up to the last day, exclude today.
        val daysForAverage = service<EngagementScorePersistenceService>().getDaysForAverage(today())

        if (daysForAverage.isEmpty()) {
            return
        }


        val activeDays = daysForAverage.size.toLong()
        val average = daysForAverage.values.average().let {
            if (it.isNaN()) 0 else it.roundToLong()
        }

        //todo: when ActivityMonitor is changed to application service this coroutine can be removed and just send the event
        //this is just a technical debt that we have:
        //ActivityMonitor is a project service, EngagementScoreService is an application service
        // and doesn't have a reference to a project, usually we use findActiveProject() to get a reference to
        // a project and get the ActivityMonitor service.
        //but this event is daily, if we don't find an active project we'll miss the event time.
        //an active project will not be found if user closed the last project exactly at this moment.
        //so we wait at least 30 minutes to find an active project, if we can't find give up and hopefully the next
        //event will be sent
        cs.launch {

            val startTime = Clock.System.now()

            var project = findActiveProject()
            while (isActive && project == null && startTime.plus(30.minutes) > Clock.System.now()) {
                delay(1.minutes.inWholeMilliseconds)
                project = findActiveProject()
            }

            project?.let {

                //update only if really sent the event
                service<EngagementScorePersistenceService>().setLastEventTime(today())
                service<EngagementScorePersistenceService>().setLatestRegisteredActiveDays(activeDays)
                service<EngagementScorePersistenceService>().setLatestRegisteredAverage(average)

                ActivityMonitor.getInstance(it).registerEngagementScore(activeDays, average)
            }
        }
    }

    private fun isDailyEventTime(): Boolean {
        return service<EngagementScorePersistenceService>().getLastEventTime()?.let {
            today() > it
        } ?: true
    }





    fun addAction(action: String) {
        cs.launch {
            if (MEANINGFUL_ACTIONS.contains(action)) {
                service<EngagementScorePersistenceService>().increment(today())
            }
        }
    }


    fun getLatestRegisteredActiveDays(): Long {
        return service<EngagementScorePersistenceService>().getLatestRegisteredActiveDays()
    }

    fun getLatestRegisteredAverage(): Long {
        return service<EngagementScorePersistenceService>().getLatestRegisteredAverage()
    }

}