package org.digma.intellij.plugin.ui;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.common.CommonUtils;
import org.digma.intellij.plugin.model.InsightType;
import org.jetbrains.annotations.NotNull;
import com.posthog.java.PostHog;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


public class ActivityMonitor implements Disposable {

    //private static final Logger LOGGER = Logger.getInstance(ActivityMonitor.class);
    private static final String POSTHOG_API_KEY = System.getenv("POSTHOG_API_KEY");
    private static final String POSTHOG_HOST = "https://app.posthog.com";
    private final PostHog posthog;
    private final String clientId;
    private LocalDateTime lastLensClick;
    private HashSet<InsightType> lastInsightsViewed;

    public ActivityMonitor() {
        posthog = new PostHog.Builder(POSTHOG_API_KEY).host(POSTHOG_HOST).build();
        clientId = Integer.toHexString(CommonUtils.getLocalHostname().hashCode());

//        String apiToken = System.getProperty("token");
//        String platformType = System.getProperty("platformType");
//
//        InputStream is = getClass().getClassLoader().getResourceAsStream("generated.txt");
//
//        try {
//            String result = IOUtils.toString(is, StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            Log.error(LOGGER, e, "");
//        }

        var osType = System.getProperty("os.name");

        var ideVersion = ApplicationInfo.getInstance().getBuild().asString();

        var pluginVersion = Optional.ofNullable(PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID)))
                .map(PluginDescriptor::getVersion)
                .orElse("unknown");

        posthog.set(clientId, new HashMap<>() {
        {
            put("os.type", osType);
            put("ide.version", ideVersion);
            put("plugin.version", pluginVersion);
        }});
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

        posthog.capture(clientId, "side-panel opened", new HashMap<>() {
        {
            put("reason", reason);
        }});
    }

    public void registerFirstConnectionEstablished() {
        posthog.capture(clientId, "connection first-established");
    }

    public void registerConnectionReestablished() {
        posthog.capture(clientId, "connection reestablished");
    }

    public void registerConnectionLost() {
        posthog.capture(clientId, "connection lost");
    }

    public void registerFirstInsightReceived() {
        posthog.capture(clientId, "insight first-received");
    }

    public void registerError(Exception exception, String message) {
        var stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        posthog.capture(clientId, "error", new HashMap<>() {
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
        posthog.capture(clientId, "insights viewed", new HashMap<>() {
        {
            put("insights", insightTypes);
        }});
    }

    public void registerInsightButtonClicked(@NotNull String button) {
        posthog.capture(clientId, "insights button-clicked", new HashMap<>() {
            {
                put("button", button);
            }});
    }

    @Override
    public void dispose() {
        posthog.shutdown();
    }
}
