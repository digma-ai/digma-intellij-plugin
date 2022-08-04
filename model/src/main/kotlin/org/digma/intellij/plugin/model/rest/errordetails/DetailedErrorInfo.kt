package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DetailedErrorInfo
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("frameStacks","stackTrace","lastInstanceCommitId")
constructor(
    val frameStacks: List<FrameStack>,
    val stackTrace: String,
    val lastInstanceCommitId: String?
)

