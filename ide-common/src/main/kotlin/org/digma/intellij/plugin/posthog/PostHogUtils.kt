package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


private val LOGGER: Logger = Logger.getInstance("PostHogUtils")
private const val POSTHOG_TOKEN_URL_RESOURCE_FILE_PATH = "posthog-token-url.txt"


internal fun getCachedToken(): String? {
    return SettingsState.getInstance().posthogToken
}

internal fun getLatestToken(): String? {
    val uri = getUriToTokenFile() ?: return null
    val newToken = readTokenFromUrl(uri) ?: return null
    return newToken
}

internal fun setCachedToken(token: String) {
    SettingsState.getInstance().posthogToken = token
    SettingsState.getInstance().fireChanged()
}

private fun readTokenFromUrl(tokenFileUri: URI): String? {
    val httpClient = HttpClient.newHttpClient()
    try {
        val response = httpClient.send(
                HttpRequest.newBuilder().GET().uri(tokenFileUri).build(),
                HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() != 200) {
            Log.log(LOGGER::warn, "Failed to read posthog token file form url (status: {})", response.statusCode())
            return null
        }
        return response.body()
    } catch (e: Exception) {
        Log.warnWithException(LOGGER, e, "Failed to read posthog token file form url")
    }
    return null
}

private fun getUriToTokenFile(): URI? {
    val content: String?
    try {
        content = object {}.javaClass.classLoader.getResource(POSTHOG_TOKEN_URL_RESOURCE_FILE_PATH)?.readText()
    } catch (e: Exception) {
        Log.warnWithException(LOGGER, e, "Failed to get posthog token resource file")
        return null
    }

    if (content == null) {
        Log.log(LOGGER::warn, "Missing posthog token resource file")
        return null
    }

    if (content.isBlank()) {
        Log.log(LOGGER::warn, "Empty posthog token resource file")
        return null
    }

    return try {
        URI.create(content)
    } catch (e: IllegalArgumentException) {
        Log.log(LOGGER::warn, "Content of posthog token resource file is not a valid uri")
        null
    }
}