package org.digma.intellij.plugin.ui;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class ActivityMonitor implements Disposable {

    //private static final Logger LOGGER = Logger.getInstance(ActivityMonitor.class);
    private static final String POSTHOG_API_KEY = System.getenv("POSTHOG_API_KEY");
    private static final String POSTHOG_HOST = "https://app.posthog.com";
    private final PostHog posthog;
    private final String clientId;
    private LocalDateTime lastLensClick;

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

        // Get the version of the JetBrains IDE
        var ideVersion = ApplicationInfo.getInstance().getBuild().asString();

        // Get a list of all installed plugins and their versions
        var pluginVersion = Arrays.stream(PluginManagerCore.getPlugins())
                .filter(x -> x.getPluginId().toString().equals(PluginId.PLUGIN_ID))
                .map(PluginDescriptor::getVersion)
                .findFirst().orElse("unknown");

        posthog.set(clientId, new HashMap<>() {
        {
            put("ide.version", ideVersion);
            put("plugin.version", pluginVersion);
        }});
    }

    public static ActivityMonitor getInstance(@NotNull Project project){
        return project.getService(ActivityMonitor.class);
    }

    public void RegisterLensClicked(){
        lastLensClick = LocalDateTime.now();
    }

    public void RegisterSidePanelOpened(){
        var reason = lastLensClick != null && Duration.between(lastLensClick, LocalDateTime.now()).getSeconds() < 2
            ? "lens click"
            : "unknown";

        posthog.capture(clientId, "side-panel opened", new HashMap<>() {
        {
            put("reason", reason);
        }});
    }

    public void RegisterFirstConnectionEstablished() {
        posthog.capture(clientId, "connection first-established");
    }

    public void RegisterConnectionReestablished() {
        posthog.capture(clientId, "connection reestablished");
    }

    public void RegisterConnectionLost() {
        posthog.capture(clientId, "connection lost");
    }

    public void RegisterFirstInsightReceived() {
        posthog.capture(clientId, "insight first-received");
    }

    public void RegisterError(Exception exception, String message) {
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

    public void RegisterInsightsViewed(@NotNull List<? extends InsightType> insightTypes) {
        posthog.capture(clientId, "insights viewed", new HashMap<>() {
        {
            put("insights", insightTypes);
        }});
    }

    @Override
    public void dispose() {
        posthog.shutdown();
    }

    public void RegisterInsightButtonClicked(@NotNull String button) {
        posthog.capture(clientId, "insights button-clicked", new HashMap<>() {
        {
            put("button", button);
        }});
    }
}
