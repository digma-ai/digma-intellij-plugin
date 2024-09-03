package org.digma.intellij.plugin.engagement

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XMap
import kotlinx.datetime.LocalDate


@State(
    name = "org.digma.intellij.plugin.engagement.EngagementScorePersistence",
    storages = [Storage("DigmaEngagementScorePersistence.xml")]
)
@Service(Service.Level.APP)
class EngagementScorePersistence : PersistentStateComponent<EngagementScoreData> {

    private var myPersistenceData = EngagementScoreData()

    override fun getState(): EngagementScoreData {
        return myPersistenceData
    }

    override fun loadState(state: EngagementScoreData) {
        myPersistenceData = state
    }
}

class EngagementScoreData {
    @OptionTag(converter = LocalDateConverter::class)
    var lastEventTime: LocalDate? = null

    //using string as day and not LocalDate because its it's a bit messy to serialise LocalDate as map keys
    // with this persistence framework.
    @get:XMap(keyAttributeName = "day", valueAttributeName = "count")
    var meaningfulActionsCounters = mutableMapOf<String, Int>()


    fun put(date: LocalDate, count: Int) {
        meaningfulActionsCounters[date.toString()] = count
    }

    fun get(date: LocalDate): Int? {
        return meaningfulActionsCounters[date.toString()]
    }

    fun increment(date: LocalDate) {
        val count = meaningfulActionsCounters[date.toString()]
        meaningfulActionsCounters[date.toString()] = count.increment()
    }

    fun remove(date: LocalDate) {
        meaningfulActionsCounters.remove(date.toString())
    }

    fun remove(date: String) {
        meaningfulActionsCounters.remove(date)
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
