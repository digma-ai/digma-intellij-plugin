package org.digma.intellij.plugin.model.rest.errordetails

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class FrameStack
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("exceptionType","frames","exceptionMessage")
constructor(
    val exceptionType: String,
    val frames: List<Frame>,
    val exceptionMessage: String
)
