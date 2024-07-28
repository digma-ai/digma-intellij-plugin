package org.digma.intellij.plugin.settings;

import com.intellij.openapi.options.*;
import com.intellij.openapi.util.NlsContexts;
import org.digma.intellij.plugin.common.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.*;
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
        return isApiUrlChanged(settings) ||
                isApiTokenChanged(settings) ||
                isJaegerUrlChanged(settings) ||
                isJaegerQueryUrlChanged(settings) ||
                isJaegerLinkModeChanged(settings) ||
                isSpringBootObservabilityModeChanged(settings) ||
                isRuntimeObservabilityBackendUrlChanged(settings) ||
                isExtendedObservabilityChanged(settings) ||
                isExtendedObservabilityExcludeChanged(settings);
    }

    private boolean isApiUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.getApiUrl(), mySettingsComponent.getApiUrl());
    }

    private boolean isApiTokenChanged(SettingsState settings) {
        return !Objects.equals(settings.getApiToken(), mySettingsComponent.getApiToken());
    }

    private boolean isJaegerUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.getJaegerUrl(), mySettingsComponent.getJaegerUrl());
    }

    private boolean isJaegerQueryUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.getJaegerQueryUrl(), mySettingsComponent.getJaegerQueryUrl());
    }

    private boolean isJaegerLinkModeChanged(SettingsState settings) {
        return !Objects.equals(settings.getJaegerLinkMode(), mySettingsComponent.getJaegerLinkMode());
    }

    private boolean isSpringBootObservabilityModeChanged(SettingsState settings) {
        return !Objects.equals(settings.getSpringBootObservabilityMode(), mySettingsComponent.getSpringBootObservabilityMode());
    }

    private boolean isRuntimeObservabilityBackendUrlChanged(SettingsState settings) {
        return !Objects.equals(settings.getRuntimeObservabilityBackendUrl(), mySettingsComponent.getRuntimeObservabilityBackendUrl());
    }

    private boolean isExtendedObservabilityChanged(SettingsState settings) {
        return !Objects.equals(settings.getExtendedObservability(), mySettingsComponent.getExtendedObservability());
    }

    private boolean isExtendedObservabilityExcludeChanged(SettingsState settings) {
        return !Objects.equals(settings.getExtendedObservabilityExcludes(), mySettingsComponent.getExtendedObservabilityExclude());
    }

    @Override
    public void apply() throws ConfigurationException {

        //run all checks and only then update the settings.
        //never update the settings before checks are complete because then the settings will have incorrect data

        try {
            Objects.requireNonNull(mySettingsComponent.getApiUrl(), "Api url can not be null");
            Objects.requireNonNull(mySettingsComponent.getRuntimeObservabilityBackendUrl(), "Runtime observability url can not be null");
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }

        if (mySettingsComponent.getApiUrl().isBlank()) {
            throw new ConfigurationException("Api url can not be empty");
        }
        if (!CommonUtils.isHttpsUrl(mySettingsComponent.getApiUrl())) {
            throw new ConfigurationException("Api url schema must be https");
        }
        try {
            URLValidator.create(mySettingsComponent.getApiUrl()).validate();
        } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                 URLValidator.IncorrectSchemaException e) {
            throw new ConfigurationException("Api url is not a well formed: " + e.getMessage());
        }

        if (mySettingsComponent.getRuntimeObservabilityBackendUrl().isBlank()) {
            throw new ConfigurationException("Runtime observability url can not be empty");
        }
        try {
            URLValidator.create(mySettingsComponent.getRuntimeObservabilityBackendUrl()).validate();
        } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                 URLValidator.IncorrectSchemaException e) {
            throw new ConfigurationException("Backend observability url is not a well formed: " + e.getMessage());
        }


        if (mySettingsComponent.getJaegerLinkMode() == JaegerLinkMode.Embedded) {
            if (mySettingsComponent.getJaegerQueryUrl() == null || mySettingsComponent.getJaegerQueryUrl().trim().isBlank()) {
                throw new ConfigurationException("Jaeger query url can not be empty in mode " + mySettingsComponent.getJaegerLinkMode());
            }
            try {
                URLValidator.create(mySettingsComponent.getJaegerQueryUrl()).validate();
            } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                     URLValidator.IncorrectSchemaException e) {
                throw new ConfigurationException("Jaeger query url is not a well formed: " + e.getMessage());
            }
        } else {
            if (mySettingsComponent.getJaegerUrl() == null || mySettingsComponent.getJaegerUrl().trim().isBlank()) {
                throw new ConfigurationException("Jaeger url can not be empty in mode " + mySettingsComponent.getJaegerLinkMode());
            }
            try {
                if (mySettingsComponent.getJaegerUrl() != null) {
                    URLValidator.create(mySettingsComponent.getJaegerUrl()).validate();
                }
            } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                     URLValidator.IncorrectSchemaException e) {
                throw new ConfigurationException("Jaeger url is not a well formed: " + e.getMessage());
            }
        }


        updateSettingsAndFireChange();
    }


    private void updateSettingsAndFireChange() {
        SettingsState settingsState = SettingsState.getInstance();
        settingsState.setApiUrl(mySettingsComponent.getApiUrl());
        var theApiToken = mySettingsComponent.getApiToken();
        if (theApiToken != null && theApiToken.isBlank()) {
            theApiToken = null;
        }
        settingsState.setApiToken(theApiToken);

        settingsState.setJaegerUrl(mySettingsComponent.getJaegerUrl());
        settingsState.setJaegerQueryUrl(mySettingsComponent.getJaegerQueryUrl());
        settingsState.setJaegerLinkMode(mySettingsComponent.getJaegerLinkMode());
        settingsState.setSpringBootObservabilityMode(mySettingsComponent.getSpringBootObservabilityMode());
        settingsState.setRuntimeObservabilityBackendUrl(mySettingsComponent.getRuntimeObservabilityBackendUrl());
        settingsState.setExtendedObservability(mySettingsComponent.getExtendedObservability());
        settingsState.setExtendedObservabilityExcludes(mySettingsComponent.getExtendedObservabilityExclude());

        settingsState.fireChanged();
    }


    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance();
        mySettingsComponent.setApiUrl(settings.getApiUrl());
        mySettingsComponent.setApiToken(settings.getApiToken());
        mySettingsComponent.setJaegerUrl(settings.getJaegerUrl());
        mySettingsComponent.setJaegerQueryUrl(settings.getJaegerQueryUrl());
        mySettingsComponent.setJaegerLinkMode(settings.getJaegerLinkMode());
        mySettingsComponent.setSpringBootObservabilityMode(settings.getSpringBootObservabilityMode());
        mySettingsComponent.setRuntimeObservabilityBackendUrl(settings.getRuntimeObservabilityBackendUrl());
        mySettingsComponent.setExtendedObservability(settings.getExtendedObservability());
        mySettingsComponent.setExtendedObservabilityExclude(settings.getExtendedObservabilityExcludes());
    }


    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
