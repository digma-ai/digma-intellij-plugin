package org.digma.intellij.plugin.engagement

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XMap
import com.jetbrains.rd.util.ConcurrentHashMap
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service(Service.Level.APP)
class EngagementScorePersistenceService {

    private val persistence = service<EngagementScorePersistence>()

    fun removeOldEntries(today: LocalDate, periodToTrack: DatePeriod) {
        persistence.removeOldEntries(today, periodToTrack)
    }

    fun getLastEventTime(): LocalDate? {
        return persistence.state.lastEventTime
    }

    fun getDaysForAverage(today: LocalDate): Map<String, Int> {
        return persistence.state.meaningfulActionsCounters.filter {
            LocalDate.parse(it.key) != today
        }
    }

    fun setLastEventTime(today: LocalDate) {
        persistence.setLastEventTime(today)
    }

    fun setLatestRegisteredActiveDays(activeDays: Long) {
        persistence.setLatestRegisteredActiveDays(activeDays)
    }

    fun setLatestRegisteredAverage(average: Long) {
        persistence.setLatestRegisteredAverage(average)
    }

    fun increment(today: LocalDate) {
        persistence.increment(today)
    }

    fun getLatestRegisteredActiveDays(): Long {
        return persistence.state.latestRegisteredActiveDays
    }

    fun getLatestRegisteredAverage(): Long {
        return persistence.state.latestRegisteredAverage
    }

}


//don't use directly, use EngagementScorePersistenceService for thread safety
@Internal
@State(
    name = "org.digma.intellij.plugin.engagement.EngagementScorePersistence",
    storages = [Storage("DigmaEngagementScorePersistence.xml")]
)
@Service(Service.Level.APP)
private class EngagementScorePersistence : PersistentStateComponent<EngagementScorePersistence.EngagementScoreData> {

    private var myPersistenceData = EngagementScoreData()

    private val lock = ReentrantLock(true)

    override fun getState(): EngagementScoreData {
        lock.withLock {
            return myPersistenceData
        }
    }

    override fun loadState(state: EngagementScoreData) {
        lock.withLock {
            myPersistenceData = state
        }
    }

    fun removeOldEntries(today: LocalDate, periodToTrack: DatePeriod) {
        lock.withLock {
            val oldEntries = myPersistenceData.meaningfulActionsCounters.keys.filter {
                LocalDate.parse(it).plus(periodToTrack) < today
            }

            oldEntries.forEach {
                myPersistenceData.remove(it)
            }
        }
    }

    fun setLastEventTime(today: LocalDate) {
        lock.withLock {
            myPersistenceData.lastEventTime = today
        }
    }

    fun setLatestRegisteredActiveDays(activeDays: Long) {
        lock.withLock {
            myPersistenceData.latestRegisteredActiveDays = activeDays
        }
    }

    fun setLatestRegisteredAverage(average: Long) {
        lock.withLock {
            myPersistenceData.latestRegisteredAverage = average
        }
    }

    fun increment(today: LocalDate) {
        lock.withLock {
            myPersistenceData.increment(today)
        }
    }


    class EngagementScoreData {
        @OptionTag(converter = LocalDateConverter::class)
        var lastEventTime: LocalDate? = null

        //using string as day and not LocalDate because it's a bit messy to serialise LocalDate as map keys
        // with this persistence framework.
        @get:XMap(keyAttributeName = "day", valueAttributeName = "count")
        var meaningfulActionsCounters = ConcurrentHashMap(mutableMapOf<String, Int>())

        var latestRegisteredActiveDays: Long = 0
        var latestRegisteredAverage: Long = 0


//        fun put(date: LocalDate, count: Int) {
//            meaningfulActionsCounters[date.toString()] = count
//        }
//
//        fun get(date: LocalDate): Int? {
//            return meaningfulActionsCounters[date.toString()]
//        }

        fun increment(date: LocalDate) {
            val count = meaningfulActionsCounters[date.toString()]
            meaningfulActionsCounters[date.toString()] = count.increment()
        }

//        fun remove(date: LocalDate) {
//            meaningfulActionsCounters.remove(date.toString())
//        }

        fun remove(date: String) {
            meaningfulActionsCounters.remove(date)
        }
    }
}

private fun Int?.increment(): Int = if (this == null) 1 else this + 1


private class LocalDateConverter : Converter<LocalDate>() {
    override fun fromString(value: String): LocalDate {
        return LocalDate.parse(value)
    }

    override fun toString(value: LocalDate): String {
        return value.toString()
    }
}
