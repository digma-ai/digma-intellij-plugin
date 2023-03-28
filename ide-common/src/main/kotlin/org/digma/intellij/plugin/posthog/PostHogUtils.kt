package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


private val LOGGER: Logger = Logger.getInstance("PostHogUtils")
private const val POSTHOG_TOKEN_URL_RESOURCE_FILE_PATH = "posthog-token-url.txt"


internal fun getCachedToken(project: Project?): String? {
    return SettingsState.getInstance(project).posthogToken
}
internal fun getLatestToken(): String? {
    val url = urlToTokenFile() ?: return null
    val newToken = readTokenFromUrl(url) ?: return null
    return newToken
}

internal fun setCachedToken(project: Project, token: String){
    SettingsState.getInstance(project).posthogToken = token
    SettingsState.getInstance(project).fireChanged()
}

private fun readTokenFromUrl(tokenFileUrl: String): String? {
    val httpClient = HttpClient.newHttpClient()
    try {
        val response = httpClient.send(
            HttpRequest.newBuilder().GET().uri(URI.create(tokenFileUrl)).build(),
            HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() != 200) {
            Log.log(LOGGER::warn, "Failed to read posthog token file form url (status: {})", response.statusCode())
            return null;
        }
        return response.body()
    } catch (e: Exception) {
        Log.warnWithException(LOGGER, e, "Failed to read posthog token file form url")
    }
    return null
}

private fun urlToTokenFile(): String? {
    try {
        val content = object{}.javaClass.classLoader.getResource(POSTHOG_TOKEN_URL_RESOURCE_FILE_PATH)?.readText()
        if (content == null) {
            Log.log(LOGGER::warn, "Missing posthog token resource file")
        }
        return content
    } catch (e: Exception) {
        Log.warnWithException(LOGGER, e, "Failed to get posthog token resource file")
        return null
    }
}