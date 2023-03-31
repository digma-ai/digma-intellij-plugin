package org.digma.intellij.plugin.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;
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

    private final JPanel myMainPanel;
    private final JBTextField myApiUrlText = new JBTextField();
    private final JBTextField myApiToken = new JBTextField();
    private final JBTextField myRefreshDelay = new JBTextField();
    private final JBTextField myJaegerUrlText = new JBTextField();
    private final JBTextField myRuntimeObservabilityBackendUrlText = new JBTextField();
    private final ComboBox<LinkMode> myJaegerLinkModeComboBox = new ComboBox<>(new EnumComboBoxModel<>(LinkMode.class));

    public SettingsComponent() {

        var myUrlLabel = new JBLabel("Digma API URL: ");
        var myUrlLabelForeground = myUrlLabel.getForeground();
        myApiUrlText.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    new URL(myApiUrlText.getText().trim());
                    myUrlLabel.setForeground(myUrlLabelForeground);
                    return true;
                } catch (MalformedURLException e) {
                    myUrlLabel.setForeground(JBColor.RED);
                    return false;
                }
            }
        });

        var myRefreshLabel = new JBLabel("Refresh every (sec.): ");
        var myRefreshLabelForeground = myRefreshLabel.getForeground();
        myRefreshDelay.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    Integer.parseInt(myRefreshDelay.getText().trim());
                    myRefreshLabel.setForeground(myRefreshLabelForeground);
                    return true;
                } catch (NumberFormatException e) {
                    myRefreshLabel.setForeground(JBColor.RED);
                    return false;
                }
            }
        });

        var myJaegerUrlLabel = new JBLabel("Jaeger URL: ");
        var myJaegerUrlForeground = myUrlLabel.getForeground();
        myJaegerUrlText.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                if (myJaegerUrlText.getText().isBlank()) {
                    myJaegerUrlLabel.setForeground(myJaegerUrlForeground);
                    return true;
                }
                try {
                    new URL(myJaegerUrlText.getText().trim());
                    myJaegerUrlLabel.setForeground(myJaegerUrlForeground);
                    return true;
                } catch (MalformedURLException e) {
                    myJaegerUrlLabel.setForeground(JBColor.RED);
                    return false;
                }
            }
        });

        var myJaegerLinkModeLabel = new JBLabel("Jaeger Link Mode: ");
        myJaegerLinkModeLabel.setToolTipText("Internal will open the link as an embedded URL within the IDE. External will open the link externally to your default browser");

        var myRuntimeObservabilityBackendUrlLabel = new JBLabel("Runtime observability backend URL:");
        myRuntimeObservabilityBackendUrlLabel.setToolTipText("Where should observability data be sent from the IDE? This would be the Digma collector URL typically listening to port 5050");
        JBLabel feedbackForRuntimeObservabilityBackendUrl = buildFeedbackNotValidUrl();

        myRuntimeObservabilityBackendUrlText.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    new URL(myRuntimeObservabilityBackendUrlText.getText().trim());
                    feedbackForRuntimeObservabilityBackendUrl.setVisible(false);
                    return true;
                } catch (MalformedURLException e) {
                    feedbackForRuntimeObservabilityBackendUrl.setVisible(true);
                    return false;
                }
            }
        });

        var resetButton = new JButton("Reset to defaults");
        resetButton.addActionListener(e -> resetToDefaults());

        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(myUrlLabel, myApiUrlText, 1, false)
                .addLabeledComponent(new JBLabel("Api token:"), myApiToken, 1, false)
                .addLabeledComponent(myRefreshLabel, myRefreshDelay, 1, false)
                .addLabeledComponent(myJaegerUrlLabel, myJaegerUrlText, 1, false)
                .addLabeledComponent(myJaegerLinkModeLabel, myJaegerLinkModeComboBox, 1, false)
                .addLabeledComponent(myRuntimeObservabilityBackendUrlLabel, myRuntimeObservabilityBackendUrlText, 1, false)
                .addComponentToRightColumn(feedbackForRuntimeObservabilityBackendUrl, 1)
                .addComponent(resetButton)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @NotNull
    private static JBLabel buildFeedbackLabel(String text, String toolTip) {
        var feedbackLabel = new JBLabel(text);
        feedbackLabel.setToolTipText(toolTip);
        feedbackLabel.setForeground(JBColor.RED);
        feedbackLabel.setVisible(false);
        return feedbackLabel;
    }

    @NotNull
    private static JBLabel buildFeedbackNotValidUrl() {
        return buildFeedbackLabel("Not a valid URL", "try to use value like http://somehost:8765");
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return myApiUrlText;
    }

    @NotNull
    public String getApiUrlText() {
        return myApiUrlText.getText().trim();
    }

    public void setApiUrlText(@NotNull String newText) {
        myApiUrlText.setText(newText.trim());
    }

    @Nullable
    public String getApiToken() {
        return myApiToken.getText().trim();
    }

    public void setApiToken(@Nullable String newText) {
        if (newText == null) {
            myApiToken.setText("");
        } else {
            myApiToken.setText(newText.trim());
        }
    }

    public String getJaegerUrl() {
        return myJaegerUrlText.getText().trim();
    }

    public void setJaegerUrl(String newText) {
        myJaegerUrlText.setText(newText.trim());
    }

    public LinkMode getJaegerLinkMode() {
        return (LinkMode) myJaegerLinkModeComboBox.getSelectedItem();
    }

    public void setJaegerLinkMode(LinkMode linkMode) {
        myJaegerLinkModeComboBox.setSelectedItem(linkMode);
    }

    @NotNull
    public String getRuntimeObservabilityBackendUrl() {
        return myRuntimeObservabilityBackendUrlText.getText().trim();
    }

    public void setRuntimeObservabilityBackendUrl(@NotNull String newText) {
        myRuntimeObservabilityBackendUrlText.setText(newText.trim());
    }

    @NotNull
    public String getRefreshDelayText() {
        return myRefreshDelay.getText().trim();
    }

    public void setRefreshDelayText(@NotNull String newText) {
        myRefreshDelay.setText(newText.trim());
    }

    private void resetToDefaults() {
        this.setApiUrlText(SettingsState.DEFAULT_API_URL);
        this.setApiToken(null);
        this.setRefreshDelayText(String.valueOf(SettingsState.DEFAULT_REFRESH_DELAY));
        this.setJaegerUrl(SettingsState.DEFAULT_JAEGER_URL);
        this.setJaegerLinkMode(SettingsState.DEFAULT_JAEGER_LINK_MODE);
        this.setRuntimeObservabilityBackendUrl(SettingsState.DEFAULT_RUNTIME_OBSERVABILITY_BACKEND_URL);
    }
}
