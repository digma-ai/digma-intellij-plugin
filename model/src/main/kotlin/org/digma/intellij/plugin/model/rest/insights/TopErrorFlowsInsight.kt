package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.InsightType
import java.beans.ConstructorProperties
import java.sql.Timestamp

data class TopErrorFlowsInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("errors")
constructor(val errors: List<Error>) : GlobalInsight {

    override val type: InsightType = InsightType.TopErrorFlows
    override fun count(): Int = errors.size

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Error
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @ConstructorProperties(
            "uid",
            "name",
            "codeObjectId",
            "sourceCodeObjectId",
            "firstOccurenceTime",
            "lastOccurenceTime",
            "handledLocally",
            "score",
            "ScoreMovingAvg",
            "ScoreTrendSlope",
            "ScoreUnhandled",
            "ScoreRecency",
            "dayAvg",
    )
    constructor(
            val uid: String,
            val name: String,
            val codeObjectId: String,
            val sourceCodeObjectId: String,
            val firstOccurenceTime: Timestamp,
            val lastOccurenceTime: Timestamp,
            val handledLocally: Boolean,
            val score: Int,
            val scoreMovingAvg: Int,
            val scoreTrendSlope: Int,
            val scoreUnhandled: Int,
            val scoreRecency: Int,
            val dayAvg: Double?,
    )
}
