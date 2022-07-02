package org.digma.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class ProjectSettings implements Configurable {

    public static final String DISPLAY_NAME = "Digma Plugin";

    private final Project project;

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
        mySettingsComponent = new SettingsComponent(project);
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance(project);
        return isUrlChanged(settings) || isUseSystemLAFChanged(settings)
                || isHtmlLabelColorChanged(settings) || isGrayedColorChanged(settings) ||
                mySettingsComponent.getIsScaleBorders() != settings.scaleBorders ||
                mySettingsComponent.getIsScalePanels() != settings.scalePanels ||
                mySettingsComponent.getIsScaleIcons() != settings.scaleIcons;
    }

    private boolean isUseSystemLAFChanged(SettingsState settings) {
        return !mySettingsComponent.isUseSystemLAF() == settings.isUseSystemLAF;
    }

    private boolean isUrlChanged(SettingsState settings){
        return !mySettingsComponent.getApiUrlText().equals(settings.apiUrl);
    }
    private boolean isHtmlLabelColorChanged(SettingsState settings){
        return !mySettingsComponent.getHtmlLabelForeground().equals(settings.htmlLabelColor);
    }

    private boolean isGrayedColorChanged(SettingsState settings){
        return !mySettingsComponent.getGrayedForeground().equals(settings.grayedColor);
    }



    @Override
    public void apply() throws ConfigurationException {
        SettingsState settings = SettingsState.getInstance(project);
        try {
            new URL(mySettingsComponent.getApiUrlText());
            Color.decode(mySettingsComponent.getHtmlLabelForeground());
            Color.decode(mySettingsComponent.getGrayedForeground());
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage(),e,e.getClass().getSimpleName());
        }
        settings.apiUrl = mySettingsComponent.getApiUrlText();
        settings.isUseSystemLAF = mySettingsComponent.isUseSystemLAF();
        settings.htmlLabelColor = mySettingsComponent.getHtmlLabelForeground();
        settings.grayedColor = mySettingsComponent.getGrayedForeground();
        settings.scalePanels = mySettingsComponent.getIsScalePanels();
        settings.scaleBorders = mySettingsComponent.getIsScaleBorders();
        settings.scaleIcons = mySettingsComponent.getIsScaleIcons();
        settings.fireChanged();
    }

    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance(project);
        mySettingsComponent.setApiUrlText(settings.apiUrl);
        mySettingsComponent.setIsUsingSystemLAF(settings.isUseSystemLAF);
        mySettingsComponent.setHtmlLabelForeground(settings.htmlLabelColor);
        mySettingsComponent.setGrayedForeground(settings.grayedColor);
        mySettingsComponent.setIsScalePanels(settings.scalePanels);
        mySettingsComponent.setIsScaleBorders(settings.scaleBorders);
        mySettingsComponent.setIsScaleIcons(settings.scaleIcons);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
