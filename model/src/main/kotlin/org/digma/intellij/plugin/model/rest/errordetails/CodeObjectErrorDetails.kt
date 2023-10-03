package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.digma.intellij.plugin.model.rest.errors.ScoreInfo
import java.beans.ConstructorProperties
import java.sql.Timestamp

// a.k.a CodeObjectErrorDetailsResponse
@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeObjectErrorDetails
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "name",
    "sourceCodeObjectId",
    "firstOccurenceTime",
    "lastOccurenceTime",
    "scoreInfo",
    "dayAvg",
    "originServices",
    "errors"
)
constructor(
    val name: String,
    val sourceCodeObjectId: String,
    val firstOccurenceTime: Timestamp,
    val lastOccurenceTime: Timestamp,
    val scoreInfo: ScoreInfo,
    val dayAvg: Int,
    val originServices: List<OriginService>,
    val errors: List<DetailedErrorInfo>
) {

    @JsonProperty("latestTraceId")
    val latestTraceId: String? = null

}
