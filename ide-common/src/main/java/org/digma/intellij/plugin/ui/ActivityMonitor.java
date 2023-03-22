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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


public class ActivityMonitor implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(ActivityMonitor.class);
    private final String clientId;
    private final PosthogClientProvider clientProvider;
    private LocalDateTime lastLensClick;
    private HashSet<InsightType> lastInsightsViewed;

    public ActivityMonitor() {
        clientId = Integer.toHexString(CommonUtils.getLocalHostname().hashCode());
        clientProvider = new PosthogClientProvider(p -> registerSessionDetails(p, clientId));
    }

    public static ActivityMonitor getInstance(@NotNull Project project){
        return project.getService(ActivityMonitor.class);
    }

    public void registerLensClicked(){
        lastLensClick = LocalDateTime.now();
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

    private static void registerSessionDetails(PostHog postHog, String clientId){
        var osType = System.getProperty("os.name");
        var ideVersion = ApplicationInfo.getInstance().getBuild().asString();
        var pluginVersion = Optional.ofNullable(PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID)))
                .map(PluginDescriptor::getVersion)
                .orElse("unknown");

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
        var posthog = clientProvider.getCurrent();
        if(posthog == null) {
            Log.log(LOGGER::debug, "Skipping posthog event registration \"{}\" (reason: client is null)", event);
            return;
        }
        posthog.capture(distinctId, event, properties);
    }

    @Override
    public void dispose() {
        clientProvider.dispose();
    }
}
