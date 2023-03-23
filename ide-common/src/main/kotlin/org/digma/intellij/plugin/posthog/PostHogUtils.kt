package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.apache.commons.io.IOUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets


private val LOGGER: Logger = Logger.getInstance("PostHogUtils")

internal fun getToken(project: Project?): String? {
    val storedToken = SettingsState.getInstance(project).posthogToken
    val url = urlToTokenFile() ?: return storedToken
    val newToken = readTokenFromUrl(url) ?: return storedToken
    if (newToken == storedToken) {
        Log.log(LOGGER::debug, "Token hasn't changed")
        return storedToken
    }
    Log.log(LOGGER::debug, "Token has changed")
    SettingsState.getInstance(project).posthogToken = newToken
    SettingsState.getInstance(project).fireChanged()
    return newToken
}

private fun readTokenFromUrl(tokenFileUrl: String): String? {
    val httpClient = HttpClient.newHttpClient()
    try {
        val response = httpClient.send(
            HttpRequest.newBuilder().GET().uri(URI.create(tokenFileUrl)).build(),
            HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() != 200) {
            Log.log(LOGGER::debug, "Failed to read posthog token file form url (status: {})", response.statusCode())
            return null;
        }
        return response.body()
    } catch (e: Exception) {
        Log.debugWithException(LOGGER, e, "Failed to read posthog token file form url")
    }
    return null
}

private fun urlToTokenFile(): String? {
    try {
        val content = object{}.javaClass.classLoader.getResource("posthog-token-url.txt")?.readText()
        if (content == null) {
            Log.log(LOGGER::debug, "Missing posthog token resource file")
        }
        return content
    } catch (e: Exception) {
        Log.debugWithException(LOGGER, e, "Failed to get posthog token resource file")
        return null
    }
}
