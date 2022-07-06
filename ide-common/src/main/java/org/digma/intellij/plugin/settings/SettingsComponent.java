// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.digma.intellij.plugin.settings;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class SettingsComponent {

  private JPanel myMainPanel;
  private final JBTextField myApiUrlText = new JBTextField();
  private final JBTextField myApiToken = new JBTextField();

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

    var resetButton = new JButton("Reset to defaults");
    resetButton.addActionListener(e -> resetToDefaults());

    myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(myUrlLabel, myApiUrlText, 1, false)
            .addLabeledComponent(new JBLabel("Api token:"), myApiToken, 1, false)
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


  private void resetToDefaults(){
    this.setApiUrlText(SettingsState.DEFAULT_API_URL);
    this.setApiToken(null);
  }
}
