package org.digma.intellij.plugin.auth

import com.intellij.openapi.diagnostic.Logger


//this login handler is selected when analytics provider is null or when there is no connection.
//errorMessage is a messages indication why it was selected
class AuthManagerNoOpLoginHandler(private val errorMessage: String) : LoginHandler {

    override val logger: Logger = Logger.getInstance(this::class.java)

    override fun loginOrRefresh(onAuthenticationError: Boolean): Boolean {
        return false
    }

    override fun login(user: String, password: String): LoginResult {
        return LoginResult(false, null, errorMessage)
    }
}