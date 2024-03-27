package org.digma.intellij.plugin.auth.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.intellij.collaboration.auth.credentials.CredentialsWithRefresh
import java.beans.ConstructorProperties
import java.util.Date

//credentials are saved in password safe storage.
// see https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html
class DigmaCredentials
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "accessToken",
    "refreshToken",
    "url",
    "tokenType",
    "expirationTime"
) constructor(
    override val accessToken: String,
    override val refreshToken: String,
    //this url is api url.
    val url: String,
    val tokenType: String,
    val expirationTime: Long,
) : CredentialsWithRefresh {

    //expiresIn and expirationTime are the same. the name expiresIn is not so clear, sounds like
    // the time left until expiration. must be implemented coz it's an interface var.
    @JsonIgnore
    override val expiresIn: Long = expirationTime

    /**
     * @return true if the token has not expired yet;
     *         false if the token has already expired.
     */
    @JsonIgnore
    override fun isAccessTokenValid(): Boolean = Date(System.currentTimeMillis()).time < expirationTime

    @JsonIgnore
    fun getExpirationTimeAsDate(): Date {
        return Date(expirationTime)
    }

    //prevent accidental logging of credential secrets
    override fun toString(): String {
        return "Credential for url $url, expiration:${getExpirationTimeAsDate()} (no details printed,see in password safe)"
    }
}