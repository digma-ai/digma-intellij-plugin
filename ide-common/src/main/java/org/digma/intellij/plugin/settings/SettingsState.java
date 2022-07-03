// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.digma.intellij.plugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "org.digma.intellij.plugin.settings.SettingsState",
        storages = @Storage("DigmaPlugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

  public static final String DEFAULT_API_URL = "https://localhost:5051";
  public static final String DEFAULT_HTML_LABEL_COLOR = "#CCCCCC";
  public static final String DEFAULT_GRAYED_COLOR = "#8A8A8A";
  public static final boolean DEFAULT_IS_USE_SYSTEM_LAF = true;
  public static final boolean DEFAULT_SCALE_PANELS = true;
  public static final boolean DEFAULT_SCALE_BORDERS = false;
  public static final boolean DEFAULT_SCALE_ICONS = true;



  public String apiUrl = DEFAULT_API_URL;
  public boolean isUseSystemLAF = DEFAULT_IS_USE_SYSTEM_LAF;

  public String htmlLabelColor = DEFAULT_HTML_LABEL_COLOR;
  public String grayedColor = DEFAULT_GRAYED_COLOR;

  private final List<SettingsChangeListener> listeners = new ArrayList<>();
  public boolean scalePanels = DEFAULT_SCALE_PANELS;
  public boolean scaleBorders = DEFAULT_SCALE_BORDERS;

  public boolean scaleIcons = DEFAULT_SCALE_ICONS;

  public static SettingsState getInstance(Project project) {
    return project.getService(SettingsState.class);
  }

  @Nullable
  @Override
  public SettingsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull SettingsState state) {
    XmlSerializerUtil.copyBean(state, this);
  }


  public void addChangeListener(SettingsChangeListener listener) {
    listeners.add(listener);
  }

  public void removeChangeListener(SettingsChangeListener listener) {
    listeners.remove(listener);
  }

  public void fireChanged(){
    listeners.forEach(listener -> {
      listener.settingsChanged(this);
    });
  }

}
