package org.digma.intellij.plugin.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.*;

import java.net.*;
import java.util.*;

import static com.intellij.util.net.NetUtils.isLocalhost;
import static org.digma.intellij.plugin.common.SettingsUtilsKt.normalizeExtendedObservabilityValue;

/**
 * Supports saving the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "org.digma.intellij.plugin.settings.SettingsState",
        storages = @Storage("DigmaPlugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState>, Disposable {

    public static final String DEFAULT_API_URL = "https://localhost:5051";
    public static final String DEFAULT_JAEGER_URL = "http://localhost:16686";
    public static final String DEFAULT_JAEGER_QUERY_URL = "http://localhost:17686";
    public static final JaegerLinkMode DEFAULT_JAEGER_LINK_MODE = JaegerLinkMode.Embedded;
    public static final SpringBootObservabilityMode DEFAULT_SPRING_BOOT_OBSERVABILITY_MODE = SpringBootObservabilityMode.OtelAgent;
    public static final String DEFAULT_RUNTIME_OBSERVABILITY_BACKEND_URL = "http://localhost:5050";


    //Note: if setters here throw exceptions it may cause an error when starting the IDE.
    // validation checks should be done elsewhere.
    //All setter and getter methods here should be public or serialization will not work.

    @NotNull
    private String apiUrl = DEFAULT_API_URL;
    @Nullable
    private String apiToken = null;
    @Nullable
    private String jaegerUrl = null;
    @Nullable
    private String jaegerQueryUrl = DEFAULT_JAEGER_QUERY_URL;
    @NotNull
    private JaegerLinkMode jaegerLinkMode = DEFAULT_JAEGER_LINK_MODE;
    @NotNull
    private SpringBootObservabilityMode springBootObservabilityMode = DEFAULT_SPRING_BOOT_OBSERVABILITY_MODE;
    @NotNull
    private String runtimeObservabilityBackendUrl = DEFAULT_RUNTIME_OBSERVABILITY_BACKEND_URL;
    @Nullable
    private String extendedObservability;
    @Nullable
    private String extendedObservabilityExcludes;


    private final List<SettingsChangeListener> listeners = new ArrayList<>();

    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Nullable
    @Override
    public SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
        if (jaegerUrlIsNotSet(jaegerUrl) &&
                apiUrlIsLocalhost(apiUrl)) {
            jaegerLinkMode = JaegerLinkMode.Embedded;
        }
    }


    private boolean apiUrlIsLocalhost(@NotNull String apiUrl) {
        try {
            var url = new URL(apiUrl);
            return isLocalhost(url.getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }


    private boolean jaegerUrlIsNotSet(String jaegerUrl) {
        return jaegerUrl == null || jaegerUrl.trim().isBlank();
    }


    //todo: add ordering, so some listener can ask to be notified before all others, add an int order and keep in sorted map,
    // for example AnalyticsUrlProvider should be the first to be notified
    public void addChangeListener(SettingsChangeListener listener, Disposable parentDisposable) {
        listeners.add(listener);

        Disposer.register(parentDisposable, () -> removeChangeListener(listener));
    }

    public void removeChangeListener(SettingsChangeListener listener) {
        listeners.remove(listener);
    }

    public void fireChanged() {
        listeners.forEach(listener -> listener.settingsChanged(this));
    }

    @Override
    public void dispose() {
        listeners.clear();
    }

    @NotNull
    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(@NotNull String newApiUrl) {
        this.apiUrl = newApiUrl;
    }

    @Nullable
    public String getJaegerUrl() {
        return jaegerUrl;
    }

    public void setJaegerUrl(@Nullable String newJaegerUrl) {
        this.jaegerUrl = newJaegerUrl;
    }

    @Nullable
    public String getJaegerQueryUrl() {
        return jaegerQueryUrl;
    }

    public void setJaegerQueryUrl(@Nullable String newJaegerQueryUrl) {
        this.jaegerQueryUrl = newJaegerQueryUrl;
    }

    @NotNull
    public String getRuntimeObservabilityBackendUrl() {
        return runtimeObservabilityBackendUrl;
    }

    public void setRuntimeObservabilityBackendUrl(@NotNull String newRuntimeObservabilityBackendUrl) {
        this.runtimeObservabilityBackendUrl = newRuntimeObservabilityBackendUrl;
    }

    @Nullable
    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(@Nullable String apiToken) {
        this.apiToken = apiToken;
    }

    @NotNull
    public JaegerLinkMode getJaegerLinkMode() {
        return jaegerLinkMode;
    }

    public void setJaegerLinkMode(@NotNull JaegerLinkMode jaegerLinkMode) {
        this.jaegerLinkMode = jaegerLinkMode;
    }

    @NotNull
    public SpringBootObservabilityMode getSpringBootObservabilityMode() {
        return springBootObservabilityMode;
    }

    public void setSpringBootObservabilityMode(@NotNull SpringBootObservabilityMode springBootObservabilityMode) {
        this.springBootObservabilityMode = springBootObservabilityMode;
    }

    @Nullable
    public String getExtendedObservability() {
        return extendedObservability;
    }

    public void setExtendedObservability(@Nullable String extendedObservability) {
        this.extendedObservability = extendedObservability;
    }

    @Nullable
    public String getExtendedObservabilityExcludes() {
        return extendedObservabilityExcludes;
    }

    public void setExtendedObservabilityExcludes(@Nullable String extendedObservabilityExcludes) {
        this.extendedObservabilityExcludes = extendedObservabilityExcludes;
    }

    @Nullable
    public String getNormalizedExtendedObservability() {
        return normalizeExtendedObservabilityValue(extendedObservability);
    }

    @Nullable
    public String getNormalizedExtendedObservabilityExcludes() {
        return normalizeExtendedObservabilityValue(extendedObservabilityExcludes);
    }

}

