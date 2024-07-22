package org.digma.intellij.plugin.auth.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.intellij.collaboration.auth.credentials.CredentialsWithRefresh
import kotlinx.datetime.Clock
import java.beans.ConstructorProperties
import java.util.Date
import kotlin.time.Duration


//credentials are saved in password safe storage.
// see https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html

@JsonIgnoreProperties(ignoreUnknown = true)
class DigmaCredentials
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "userId",
    "accessToken",
    "refreshToken",
    "url",
    "tokenType",
    "expirationTime",
    "creationTime"
) constructor(
    val userId: String,
    override val accessToken: String,
    override val refreshToken: String,
    //this url is api url.
    val url: String,
    val tokenType: String,
    val expirationTime: Long,
    //creationTime is added to this class after users already have credentials,
    // so it should be nullable for backwards compatibility.
    //todo: can be made non-nullable after few releases
    val creationTime: Long?
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
        return "Credential for url:$url, expiration:${getExpirationTimeAsDate()} (no details printed,see in password safe)"
    }

    //check if these credentials was created earlier than seconds ago
    @JsonIgnore
    fun isOlderThen(seconds: Duration): Boolean {
        if (creationTime == null) {
            return true
        }

        return (creationTime + seconds.inWholeMilliseconds) < Clock.System.now().toEpochMilliseconds()
    }
}