// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.digma.intellij.plugin.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class SettingsComponent {

  private final JPanel myMainPanel;
  private final JBTextField myApiUrlText = new JBTextField();

  public SettingsComponent() {

    var myLabel = new JBLabel("Digma Api url (requires IDE restart): ");
    var myLabelForeground = myLabel.getForeground();
    myApiUrlText.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        try {
          new URL(myApiUrlText.getText());
          myLabel.setForeground(myLabelForeground);
          return true;
        } catch (MalformedURLException e) {
          myLabel.setForeground(Color.RED);
          return false;
        }
      }
    });

    myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(myLabel, myApiUrlText, 1, false)
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
  public String getApiUrlText()  {
    return myApiUrlText.getText();
  }

  public void setApiUrlText(@NotNull String newText) {
    myApiUrlText.setText(newText);
  }


}
