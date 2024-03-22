package org.digma.intellij.plugin.auth.credentials

import com.intellij.collaboration.auth.credentials.CredentialsWithRefresh
import java.util.Date

class DigmaCredentials(
    override val accessToken: String,
    override val refreshToken: String,
    override val expiresIn: Long,
    //this url is the token url, not the base url.
    val url: String,
    val tokenType: String,
    val expirationTime: Long,
) : CredentialsWithRefresh {


    /**
     * @return true if the token has not expired yet;
     *         false if the token has already expired.
     */
    override fun isAccessTokenValid(): Boolean = Date(System.currentTimeMillis()).time < expirationTime
}