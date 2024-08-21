package org.digma.intellij.plugin.auth.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.intellij.collaboration.auth.credentials.CredentialsWithRefresh
import kotlinx.datetime.Clock
import org.apache.commons.codec.digest.DigestUtils
import java.beans.ConstructorProperties
import java.util.Date
import kotlin.math.max
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
    //this url is the api url.
    val url: String,
    val tokenType: String,
    val expirationTime: Long,
    private val creationTime: Long
) : CredentialsWithRefresh {

    //the format of expirationTime from the server is 2024-08-18T21:33:24Z


    //expiresIn is the time left for expiration, it is not accurate because it is assigned some time after
    // the server created the expirationTime. it should actually be
    // (expirationTime - network time - now) but we can't calculate it.
    // we don't use it , it must be implemented.
    // we could just use it for the expirationTime but the name will be confusing, our server sends expirationTime.
    @JsonIgnore
    override val expiresIn: Long = max(0, expirationTime - Clock.System.now().toEpochMilliseconds())

    /**
     * @return true if the token has not expired yet;
     *         false if the token has already expired.
     */
    @JsonIgnore
    override fun isAccessTokenValid(): Boolean = Clock.System.now().toEpochMilliseconds() < expirationTime

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
    fun isOlderThen(duration: Duration): Boolean {
        return (creationTime + duration.inWholeMilliseconds) < Clock.System.now().toEpochMilliseconds()
    }

    @JsonIgnore
    fun isYoungerThen(seconds: Duration): Boolean {
        return !isOlderThen(seconds)
    }

    @JsonIgnore
    fun accessTokenHash(): String {
        return DigestUtils.sha1Hex(accessToken)
    }

    @JsonIgnore
    fun refreshTokenHash(): String {
        return DigestUtils.sha1Hex(refreshToken)
    }



}