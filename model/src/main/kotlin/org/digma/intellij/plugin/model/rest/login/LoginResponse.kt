package org.digma.intellij.plugin.model.rest.login

import com.fasterxml.jackson.annotation.JsonCreator
import java.beans.ConstructorProperties
import java.util.Date


data class LoginResponse
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "accessToken",
    "refreshToken",
    "expiration"
) constructor(
    val accessToken: String,
    val refreshToken: String,
    val expiration: Date,
)
