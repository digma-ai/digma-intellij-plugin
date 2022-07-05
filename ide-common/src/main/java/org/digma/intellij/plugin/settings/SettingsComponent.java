// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.digma.intellij.plugin.settings;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColorChooserService;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.digma.intellij.plugin.ui.DigmaUIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class SettingsComponent {

  private JPanel myMainPanel;
  private final JBTextField myApiUrlText = new JBTextField();
  private final JBTextField myApiToken = new JBTextField();
  private final JBCheckBox myIsUsingSystemLAF = new JBCheckBox();
  private final JBCheckBox myIsScalePanel = new JBCheckBox();
  private final JBCheckBox myIsScaleBorders = new JBCheckBox();
  private final JBCheckBox myIsScaleIcons = new JBCheckBox();

  private final JButton htmlForegroundColorPicker = new JButton("Select foreground color for html labels");
  private String htmlLabelForeground = "#CCCCCC";
  private final JButton grayedColorPicker = new JButton("Select grayed color for labels");
  private String grayedForeground = "#8A8A8A";

  public SettingsComponent(Project project) {

    var myUrlLabel = new JBLabel("Digma Api url: ");
    var myLabelForeground = myUrlLabel.getForeground();
    myApiUrlText.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        try {
          new URL(myApiUrlText.getText());
          myUrlLabel.setForeground(myLabelForeground);
          return true;
        } catch (MalformedURLException e) {
          myUrlLabel.setForeground(JBColor.RED);
          return false;
        }
      }
    });


    htmlForegroundColorPicker.setForeground(Color.decode(htmlLabelForeground));
    htmlForegroundColorPicker.addActionListener(e -> {
      if (myMainPanel != null) {
        Color c = ColorChooserService.getInstance().showDialog(project,htmlForegroundColorPicker, "Choose Color", Color.decode(htmlLabelForeground), true, new ArrayList<>(), true);
        if (c != null) {
          htmlForegroundColorPicker.setForeground(c);
          htmlLabelForeground = DigmaUIUtil.digmaColorToHex(c);
        }
      }
    });


    grayedColorPicker.setForeground(Color.decode(grayedForeground));
    grayedColorPicker.addActionListener(e -> {
      if (myMainPanel != null) {
        Color c = ColorChooserService.getInstance().showDialog(project,grayedColorPicker, "Choose Color", Color.decode(grayedForeground), true, new ArrayList<>(), true);
        if (c != null) {
          grayedColorPicker.setForeground(c);
          grayedForeground = DigmaUIUtil.digmaColorToHex(c);
        }
      }
    });


    myIsUsingSystemLAF.addActionListener(e -> htmlForegroundColorPicker.setEnabled(!myIsUsingSystemLAF.isSelected()));

    var myIsUsingSystemLAFLabel = new JBLabel("Use system LAF for html labels ? ");


    var resetButton = new JButton("Reset to defaults");
    resetButton.addActionListener(e -> resetToDefaults());

    myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(myUrlLabel, myApiUrlText, 1, false)
            .addLabeledComponent(new JBLabel("Api token:"), myApiToken, 1, false)
//            .addLabeledComponent(myIsUsingSystemLAFLabel, myIsUsingSystemLAF, 1, false)
//            .addComponentToRightColumn(htmlForegroundColorPicker, 1)
//            .addComponentToRightColumn(grayedColorPicker, 1)
//            .addLabeledComponent(new JBLabel("Scale panels ?"), myIsScalePanel, 1, false)
//            .addLabeledComponent(new JBLabel("Scale borders ?"), myIsScaleBorders, 1, false)
//            .addLabeledComponent(new JBLabel("Scale icons ?"), myIsScaleIcons, 1, false)
            .addComponent(resetButton)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();


  }

  public JPanel getPanel() {
    return myMainPanel;
  }

  public JComponent getPreferredFocusedComponent() {
    return myApiUrlText;
  }

  @NotNull
  public String getApiUrlText() {
    return myApiUrlText.getText();
  }

  public void setApiUrlText(@NotNull String newText) {
    myApiUrlText.setText(newText);
  }

  @Nullable
  public String getApiToken() {
    return myApiToken.getText();
  }

  public void setApiToken(@Nullable String newText) {
    myApiToken.setText(newText);
  }


  public boolean isUseSystemLAF() {
    return myIsUsingSystemLAF.isSelected();
  }

  public void setIsUsingSystemLAF(boolean newIsUseSystemLAF) {
    if (myIsUsingSystemLAF.isSelected() != newIsUseSystemLAF) {
      myIsUsingSystemLAF.doClick();
    }
  }

  public String getHtmlLabelForeground() {
    return htmlLabelForeground;
  }

  public void setHtmlLabelForeground(String htmlLabelForeground) {
    this.htmlLabelForeground = htmlLabelForeground;
    this.htmlForegroundColorPicker.setForeground(Color.decode(htmlLabelForeground));
  }

  public String getGrayedForeground() {
    return grayedForeground;
  }

  public void setGrayedForeground(String grayedForeground) {
    this.grayedForeground = grayedForeground;
    this.grayedColorPicker.setForeground(Color.decode(grayedForeground));
  }


  public boolean getIsScalePanels(){
    return myIsScalePanel.isSelected();
  }
  public void setIsScalePanels(boolean scale){
    myIsScalePanel.setSelected(scale);
  }

  public boolean getIsScaleBorders(){
    return myIsScaleBorders.isSelected();
  }
  public void setIsScaleBorders(boolean scale){
    myIsScaleBorders.setSelected(scale);
  }

  public void setIsScaleIcons(boolean scale) {
    myIsScaleIcons.setSelected(scale);
  }

  public boolean getIsScaleIcons() {
    return myIsScaleIcons.isSelected();
  }


  private void resetToDefaults(){
    this.setApiUrlText(SettingsState.DEFAULT_API_URL);
    this.setApiToken(null);
    this.setIsUsingSystemLAF(SettingsState.DEFAULT_IS_USE_SYSTEM_LAF);
    this.setHtmlLabelForeground(SettingsState.DEFAULT_HTML_LABEL_COLOR);
    this.setGrayedForeground(SettingsState.DEFAULT_GRAYED_COLOR);
    this.setIsScalePanels(SettingsState.DEFAULT_SCALE_PANELS);
    this.setIsScaleBorders(SettingsState.DEFAULT_SCALE_BORDERS);
    this.setIsScaleIcons(SettingsState.DEFAULT_SCALE_ICONS);
  }


}
