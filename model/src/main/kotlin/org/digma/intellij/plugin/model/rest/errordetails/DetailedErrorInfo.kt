package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.beans.ConstructorProperties

// a.k.a ErrorFlowInfo
@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailedErrorInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("frameStacks","stackTrace","lastInstanceCommitId")
constructor(
    val frameStacks: List<FrameStack>,
    val stackTrace: String,
    val lastInstanceCommitId: String?
) {

    @JsonProperty("latestTraceId")
    val latestTraceId: String? = null

}

