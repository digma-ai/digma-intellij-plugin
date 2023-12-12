package org.digma.intellij.plugin.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.digma.intellij.plugin.common.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.util.net.NetUtils.isLocalhost;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "org.digma.intellij.plugin.settings.SettingsState",
        storages = @Storage("DigmaPlugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState>, Disposable {

    public static final String DEFAULT_API_URL = "https://localhost:5051";
    public static final int DEFAULT_REFRESH_DELAY = ApplicationManager.getApplication().isUnitTestMode() ? 1 : 10; // when the delay is 10 seconds it, the test will get a timeout on the start of the system test.
    public static final String DEFAULT_JAEGER_URL = "http://localhost:16686";
    public static final String DEFAULT_JAEGER_QUERY_URL = "http://localhost:17686";
    public static final LinkMode DEFAULT_JAEGER_LINK_MODE = LinkMode.Embedded;
    public static final SpringBootObservabilityMode DEFAULT_SPRING_BOOT_OBSERVABILITY_MODE = SpringBootObservabilityMode.OtelAgent;
    public static final String DEFAULT_RUNTIME_OBSERVABILITY_BACKEND_URL = "http://localhost:5050";

    @NotNull
    public String apiUrl = DEFAULT_API_URL;
    public int refreshDelay = DEFAULT_REFRESH_DELAY;
    @Nullable
    public String apiToken = null;
    @Nullable
    public String jaegerUrl = "";
    @NotNull
    public String jaegerQueryUrl = DEFAULT_JAEGER_QUERY_URL;
    @NotNull
    public LinkMode jaegerLinkMode = DEFAULT_JAEGER_LINK_MODE;
    @NotNull
    public SpringBootObservabilityMode springBootObservabilityMode = DEFAULT_SPRING_BOOT_OBSERVABILITY_MODE;
    @NotNull
    public String runtimeObservabilityBackendUrl = DEFAULT_RUNTIME_OBSERVABILITY_BACKEND_URL;
    @Nullable
    public String posthogToken;

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
                apiUrlIsLocalhost(apiUrl)){
            jaegerLinkMode = LinkMode.Embedded;
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
        return !CommonUtils.isWelFormedUrl(jaegerUrl);
    }


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
}
