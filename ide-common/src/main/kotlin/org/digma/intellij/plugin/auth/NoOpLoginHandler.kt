package org.digma.intellij.plugin.auth

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log


//this login handler is selected when analytics provider is null or when there is no connection.
//errorMessage is a messages indication why it was selected
class NoOpLoginHandler(private val errorMessage: String) : LoginHandler {

    override val logger: Logger = Logger.getInstance(this::class.java)

    override fun loginOrRefresh(onAuthenticationError: Boolean): Boolean {
        Log.log(logger::trace, "loginOrRefresh called, error message {}", errorMessage)
        return false
    }

    override fun login(user: String, password: String): LoginResult {
        Log.log(logger::trace, "login called, error message {}", errorMessage)
        return LoginResult(false, null, errorMessage)
    }
}