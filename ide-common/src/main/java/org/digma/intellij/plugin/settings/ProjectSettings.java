package org.digma.intellij.plugin.settings;

import com.intellij.openapi.options.*;
import com.intellij.openapi.util.NlsContexts;
import org.digma.intellij.plugin.common.CommonUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class ProjectSettings implements Configurable {

    public static final String DISPLAY_NAME = "Digma Plugin";

    private SettingsComponent mySettingsComponent;

    public ProjectSettings() {
        super();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Override
    public @Nullable JComponent createComponent() {
        mySettingsComponent = new SettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();
        return isUrlChanged(settings) ||
                isApiTokenChanged(settings) ||
                isRefreshDelayChanged(settings) ||
                isJaegerUrlChanged(settings) ||
                isJaegerQueryUrlChanged(settings) ||
                isJaegerLinkModeChanged(settings) ||
                isSpringBootObservabilityModeChanged(settings) ||
                isRuntimeObservabilityBackendUrlChanged(settings) ||
                isExtendedObservabilityChanged(settings);
    }

    private boolean isRefreshDelayChanged(SettingsState settings) {
        return !Objects.equals(String.valueOf(settings.refreshDelay), mySettingsComponent.getRefreshDelayText());
    }

    private boolean isUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.apiUrl, mySettingsComponent.getApiUrlText());
    }

    private boolean isApiTokenChanged(SettingsState settings) {
        return !Objects.equals(settings.apiToken, mySettingsComponent.getApiToken());
    }

    private boolean isJaegerUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.jaegerUrl, mySettingsComponent.getJaegerUrl());
    }

    private boolean isJaegerQueryUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.jaegerQueryUrl, mySettingsComponent.getJaegerQueryUrl());
    }

    private boolean isJaegerLinkModeChanged(SettingsState settings) {
        return !Objects.equals(settings.jaegerLinkMode, mySettingsComponent.getJaegerLinkMode());
    }

    private boolean isSpringBootObservabilityModeChanged(SettingsState settings) {
        return !Objects.equals(settings.springBootObservabilityMode, mySettingsComponent.getSpringBootObservabilityMode());
    }

    private boolean isRuntimeObservabilityBackendUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.runtimeObservabilityBackendUrl, mySettingsComponent.getRuntimeObservabilityBackendUrl());
    }

    private boolean isExtendedObservabilityChanged(SettingsState settings) {
        return !Objects.equals(settings.extendedObservability, mySettingsComponent.getExtendedObservability());
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        try {
            Objects.requireNonNull(mySettingsComponent.getApiUrlText(), "api url can not be null");
            Objects.requireNonNull(mySettingsComponent.getRuntimeObservabilityBackendUrl(), "Runtime observability url can not be null");
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage(), e, e.getClass().getSimpleName());
        }

        if (mySettingsComponent.getApiUrlText().isBlank()) {
            throw new ConfigurationException("Api url can not be empty");
        }

        if (mySettingsComponent.getRuntimeObservabilityBackendUrl().isBlank()) {
            throw new ConfigurationException("Runtime observability url can not be empty");
        }


        if (!CommonUtils.isWelFormedUrl(mySettingsComponent.getApiUrlText())) {
            throw new ConfigurationException("Api url is not a well formed url");
        }

        if (!CommonUtils.isHttpsUrl(mySettingsComponent.getApiUrlText())) {
            throw new ConfigurationException("Api url schema must be https");
        }

        if (!CommonUtils.isWelFormedUrl(mySettingsComponent.getRuntimeObservabilityBackendUrl())) {
            throw new ConfigurationException("Runtime observability is not a well formed url");
        }

        if (mySettingsComponent.getJaegerUrl() != null &&
                !mySettingsComponent.getJaegerUrl().isBlank() &&
                !CommonUtils.isWelFormedUrl(mySettingsComponent.getJaegerUrl())) {
            throw new ConfigurationException("Jaeger url is not a well formed url");
        }


        if (mySettingsComponent.getJaegerLinkMode() == LinkMode.Embedded) {
            if (!CommonUtils.isWelFormedUrl(mySettingsComponent.getJaegerQueryUrl())) {
                throw new ConfigurationException("Jaeger query url must be well formed in Embedded mode");
            }
        } else {
            if (mySettingsComponent.getJaegerQueryUrl() != null &&
                    !mySettingsComponent.getJaegerQueryUrl().isBlank() &&
                    !CommonUtils.isWelFormedUrl(mySettingsComponent.getJaegerQueryUrl())) {
                throw new ConfigurationException("Jaeger query url is not a well formed url");
            }
        }

        var theApiToken = mySettingsComponent.getApiToken();
        if (theApiToken != null && theApiToken.isBlank()) {
            theApiToken = null;
        }

        settings.apiUrl = mySettingsComponent.getApiUrlText();
        settings.apiToken = theApiToken;
        settings.refreshDelay = Integer.parseInt(mySettingsComponent.getRefreshDelayText());
        settings.jaegerUrl = mySettingsComponent.getJaegerUrl();
        settings.jaegerQueryUrl = mySettingsComponent.getJaegerQueryUrl();
        settings.jaegerLinkMode = mySettingsComponent.getJaegerLinkMode();
        settings.springBootObservabilityMode = mySettingsComponent.getSpringBootObservabilityMode();
        settings.runtimeObservabilityBackendUrl = mySettingsComponent.getRuntimeObservabilityBackendUrl();
        settings.extendedObservability = mySettingsComponent.getExtendedObservability();
        settings.fireChanged();
    }

    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance();
        mySettingsComponent.setApiUrlText(settings.apiUrl);
        mySettingsComponent.setApiToken(settings.apiToken);
        mySettingsComponent.setRefreshDelayText(String.valueOf(settings.refreshDelay));
        mySettingsComponent.setJaegerUrl(settings.jaegerUrl);
        mySettingsComponent.setJaegerQueryUrl(settings.jaegerQueryUrl);
        mySettingsComponent.setJaegerLinkMode(settings.jaegerLinkMode);
        mySettingsComponent.setSpringBootObservabilityMode(settings.springBootObservabilityMode);
        mySettingsComponent.setRuntimeObservabilityBackendUrl(settings.runtimeObservabilityBackendUrl);
        mySettingsComponent.setExtendedObservability(settings.extendedObservability);
    }


    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
