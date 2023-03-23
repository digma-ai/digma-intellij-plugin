package org.digma.intellij.plugin.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.io.IOUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.settings.SettingsState;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class PostHogTokenProvider {

    private static final Logger LOGGER = Logger.getInstance(ActivityMonitor.class);

    public static @Nullable String GetToken(Project project) {
        var storedToken = SettingsState.getInstance(project).posthogToken;

        var url = getUrlToTokenFile();
        if(url == null)
            return storedToken;

        var newToken = readTokenFromUrl(url);
        if(newToken == null)
            return storedToken;

        if(newToken.equals(storedToken)){
            Log.log(LOGGER::debug, "Token hasn't changed");
            return storedToken;
        }

        Log.log(LOGGER::debug, "Token has changed");
        SettingsState.getInstance(project).posthogToken = newToken;
        SettingsState.getInstance(project).fireChanged();
        return newToken;
    }

    private static @Nullable String readTokenFromUrl(String tokenFileUrl){
        var httpClient = HttpClient.newHttpClient();
        try {
            var response = httpClient.send(
                    HttpRequest.newBuilder().GET().uri(URI.create(tokenFileUrl)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
            {
                Log.log(LOGGER::debug, "Failed to read posthog token file form url (status: {})", response.statusCode());
                return null;
            }
            return response.body();
        } catch (Exception e) {
            Log.debugWithException(LOGGER, e, "Failed to read posthog token file form url");
            return null;
        }
    }

    private static @Nullable String getUrlToTokenFile(){
        try {
            InputStream is = PostHogTokenProvider.class.getClassLoader().getResourceAsStream("posthog-token-url.txt");
            if(is == null)
            {
                Log.log(LOGGER::debug, "Missing posthog token resource file");
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.debugWithException(LOGGER, e, "Failed to get posthog token resource file");
            return null;
        }
    }
}
