package org.digma.intellij.plugin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.posthog.java.PostHog;
import org.apache.commons.io.IOUtils;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class PosthogClientProvider extends TimerTask implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(ActivityMonitor.class);
    private final Consumer<PostHog> onFirstConnection;
    private String token;
    private PostHog posthog;

    public PosthogClientProvider(Consumer<PostHog> onFirstConnection){
        this.onFirstConnection = onFirstConnection;
        new Timer().scheduleAtFixedRate(this, 0, 1000*60*5 /*5min*/ );
    }

    public @Nullable PostHog getCurrent(){
        return posthog;
    }

    @Override
    public void run() {
        var url = getUrlToTokenFile();
        if(url == null)
            return;

        var newToken = readTokenFromUrl(url);
        if(newToken == null)
            return;

        if(newToken.equals(token))
            return;

        var prevPosthog = posthog;
        posthog = new PostHog.Builder(newToken).build();
        token = newToken;

        if(prevPosthog == null){
            onFirstConnection.accept(posthog);
        }
    }

    @Override
    public void dispose() {
        cancel();
        if(posthog != null)
            posthog.shutdown();
    }

    private @Nullable String readTokenFromUrl(String tokenFileUrl){
        var httpClient = HttpClient.newHttpClient();
        try {
            var response = httpClient.send(HttpRequest.newBuilder().GET().uri(URI.create(tokenFileUrl)).build(), HttpResponse.BodyHandlers.ofString());
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

    private @Nullable String getUrlToTokenFile(){
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("posthog-token-url.txt");
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
