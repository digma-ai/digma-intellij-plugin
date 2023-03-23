package org.digma.intellij.plugin.ui;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.posthog.java.PostHog;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.common.CommonUtils;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


public class ActivityMonitor implements Runnable, Disposable {

    private static final Logger LOGGER = Logger.getInstance(ActivityMonitor.class);
    private final String clientId;
    private final Project project;
    private final Thread tokenFetcherThread;
    @Nullable
    private PostHog postHog;
    private LocalDateTime lastLensClick;
    private HashSet<InsightType> lastInsightsViewed;

    public ActivityMonitor(Project project) {
        this.project = project;
        clientId = Integer.toHexString(CommonUtils.getLocalHostname().hashCode());
        tokenFetcherThread = new Thread(this, "Token fetcher thread");
        tokenFetcherThread.start();
    }

    public static ActivityMonitor getInstance(@NotNull Project project){
        return project.getService(ActivityMonitor.class);
    }

    @Override
    public void run() {
        var token = PostHogTokenProvider.GetToken(project);
        if(token != null)
        {
            postHog = new PostHog.Builder(token).build();
            registerSessionDetails();
            registerPluginLoaded();
        }
    }

    public void registerLensClicked(){
        lastLensClick = LocalDateTime.now();
    }

    public void registerPluginLoaded(){
        capture(clientId, "plugin loaded");
    }

    public void registerSidePanelOpened(){
        var reason = lastLensClick != null && Duration.between(lastLensClick, LocalDateTime.now()).getSeconds() < 2
            ? "lens click"
            : "unknown";

        capture(clientId, "side-panel opened", new HashMap<>() {
        {
            put("reason", reason);
        }});
    }

    public void registerFirstConnectionEstablished() {
        capture(clientId, "connection first-established");
    }

    public void registerConnectionReestablished() {
        capture(clientId, "connection reestablished");
    }

    public void registerConnectionLost() {
        capture(clientId, "connection lost");
    }

    public void registerFirstInsightReceived() {
        capture(clientId, "insight first-received");
    }

    public void registerError(Exception exception, String message) {
        var stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        capture(clientId, "error", new HashMap<>() {
            {
                put("message", message);
                put("exception.type", exception.getClass().getName());
                put("exception.message", exception.getMessage());
                put("exception.stack-trace", stringWriter.toString());
            }});
    }

    public void registerInsightsViewed(@NotNull List<? extends InsightType> insightTypes) {
        var newInsightsViewed = new HashSet<InsightType>(insightTypes);
        if(lastInsightsViewed != null && lastInsightsViewed.equals(newInsightsViewed))
            return;

        lastInsightsViewed = newInsightsViewed;
        capture(clientId, "insights viewed", new HashMap<>() {
        {
            put("insights", insightTypes);
        }});
    }

    public void registerInsightButtonClicked(@NotNull String button) {
        capture(clientId, "insights button-clicked", new HashMap<>() {
        {
            put("button", button);
        }});
    }

    private void registerSessionDetails(){
        var osType = System.getProperty("os.name");
        var ideVersion = ApplicationInfo.getInstance().getBuild().asString();
        var pluginVersion = Optional.ofNullable(PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID)))
                .map(PluginDescriptor::getVersion)
                .orElse("unknown");

        if(postHog == null)
            return;

        postHog.set(clientId, new HashMap<>() {
        {
            put("os.type", osType);
            put("ide.version", ideVersion);
            put("plugin.version", pluginVersion);
        }});
    }

    private void capture(String distinctId, String event) {
        capture(distinctId, event, null);
    }

    private void capture(String distinctId, String event, Map<String, Object> properties) {

        if(postHog == null) {
            Log.log(LOGGER::debug, "Skipping posthog event registration \"{}\" (reason: client is null)", event);
            return;
        }
        postHog.capture(distinctId, event, properties);
    }

    @Override
    public void dispose() {
        try {
            tokenFetcherThread.join();
        } catch (InterruptedException e) {
            Log.debugWithException(LOGGER, e, "Failed waiting for tokenFetcherThread");
        }
    }
}
