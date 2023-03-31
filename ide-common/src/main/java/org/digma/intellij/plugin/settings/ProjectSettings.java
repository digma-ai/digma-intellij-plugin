package org.digma.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.URL;
import java.util.Objects;

public class ProjectSettings implements Configurable {

    public static final String DISPLAY_NAME = "Digma Plugin";

    private SettingsComponent mySettingsComponent;

    public ProjectSettings() {
        super();
    }

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
        return isUrlChanged(settings) || isApiTokenChanged(settings) || isRefreshDelayChanged(settings)
                || isJaegerUrlChanged(settings) || isJaegerLinkModeChanged(settings)
                || isRuntimeObservabilityBackendUrl(settings);
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

    private boolean isJaegerLinkModeChanged(SettingsState settings) {
        return !Objects.equals(settings.jaegerLinkMode, mySettingsComponent.getJaegerLinkMode());
    }

    private boolean isRuntimeObservabilityBackendUrl(SettingsState settings) {
        return !Objects.equals(settings.runtimeObservabilityBackendUrl, mySettingsComponent.getRuntimeObservabilityBackendUrl());
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance();
        try {
            Objects.requireNonNull(mySettingsComponent.getApiUrlText(), "api url can not be null");
            new URL(mySettingsComponent.getApiUrlText());
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage(), e, e.getClass().getSimpleName());
        }

        if (mySettingsComponent.getApiUrlText().isBlank()) {
            throw new ConfigurationException("Api url can not be blank");
        }

        var theApiToken = mySettingsComponent.getApiToken();
        if (theApiToken != null && theApiToken.isBlank()) {
            theApiToken = null;
        }


        settings.apiUrl = mySettingsComponent.getApiUrlText();
        settings.apiToken = theApiToken;
        settings.refreshDelay = Integer.parseInt(mySettingsComponent.getRefreshDelayText());
        settings.jaegerUrl = mySettingsComponent.getJaegerUrl();
        settings.jaegerLinkMode = mySettingsComponent.getJaegerLinkMode();
        settings.runtimeObservabilityBackendUrl = mySettingsComponent.getRuntimeObservabilityBackendUrl();
        settings.fireChanged();
    }

    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance();
        mySettingsComponent.setApiUrlText(settings.apiUrl);
        mySettingsComponent.setApiToken(settings.apiToken);
        mySettingsComponent.setRefreshDelayText(String.valueOf(settings.refreshDelay));
        mySettingsComponent.setJaegerUrl(settings.jaegerUrl);
        mySettingsComponent.setJaegerLinkMode(settings.jaegerLinkMode);
        mySettingsComponent.setRuntimeObservabilityBackendUrl(settings.runtimeObservabilityBackendUrl);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
