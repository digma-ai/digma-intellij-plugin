package org.digma.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

public class ProjectSettings implements Configurable {

    public static final String DISPLAY_NAME = "Digma Plugin";

    private Project project;

    private SettingsComponent mySettingsComponent;

    public ProjectSettings(Project project) {
        this.project = project;
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
        SettingsState settings = SettingsState.getInstance(project);
        return !mySettingsComponent.getApiUrlText().equals(settings.apiUrl);
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance(project);
        try {
            new URL(mySettingsComponent.getApiUrlText());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("URL is malformed",e,"MalformedURLException");
        }
        settings.apiUrl = mySettingsComponent.getApiUrlText();
    }

    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance(project);
        mySettingsComponent.setApiUrlText(settings.apiUrl);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
