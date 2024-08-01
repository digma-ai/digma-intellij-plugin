package org.digma.intellij.plugin.auth

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.account.LoginHandler
import org.digma.intellij.plugin.auth.account.LoginResult
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.log.Log


//this login handler is selected when analytics provider is null or when there is no connection.
//errorMessage is a messages indication why it was selected
class NoOpLoginHandler(private val errorMessage: String) : LoginHandler {

    override val logger: Logger = Logger.getInstance(this::class.java)

    override suspend fun loginOrRefresh(onAuthenticationError: Boolean, trigger: String): Boolean {
        Log.log(logger::trace, "loginOrRefresh called, error message {}, trigger {}", errorMessage, trigger)
        return false
    }

    override suspend fun login(user: String, password: String, trigger: String): LoginResult {
        Log.log(logger::trace, "login called, error message {}, trigger {}", errorMessage, trigger)
        return LoginResult(false, null, errorMessage)
    }

    override suspend fun refresh(account: DigmaAccount, credentials: DigmaCredentials, trigger: String): Boolean {
        Log.log(logger::trace, "refresh called, error message {}, trigger {}", errorMessage, trigger)
        return false
    }
}