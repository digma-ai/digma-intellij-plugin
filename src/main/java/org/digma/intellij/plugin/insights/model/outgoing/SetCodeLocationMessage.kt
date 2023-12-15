package org.digma.intellij.plugin.insights.model.outgoing

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class SetCodeLocationData @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("codeLocations")
constructor(
    val codeLocations: List<String>,
)


data class SetCodeLocationMessage(val type: String = "digma", val action: String = "INSIGHTS/SET_CODE_LOCATIONS", val payload: SetCodeLocationData)

