package org.digma.intellij.plugin.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.*;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.Objects;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class SettingsComponent {

    private final JPanel myMainPanel;
    private final JBTextField myApiUrlText = new JBTextField();
    private final JBTextField myApiToken = new JBTextField();
    private final JBTextField myRefreshDelay = new JBTextField();
    private final JBTextField myJaegerUrlText = new JBTextField();
    private final JBLabel myJaegerUrlLabel = new JBLabel("Jaeger URL: (For internal/external mode)");
    private final JBTextField myJaegerQueryUrlText = new JBTextField();
    private final JBLabel myJaegerQueryUrlLabel = new JBLabel("Jaeger Query URL (For embedded mode): ");
    private final ComboBox<LinkMode> myJaegerLinkModeComboBox = new ComboBox<>(new EnumComboBoxModel<>(LinkMode.class));
    private final JBLabel myEmbeddedJaegerMessage = new JBLabel("<html><body><span style=\"color:\"" + JBColor.BLUE + "\"\"><b>Jaeger embedded is only supported for deployment on a local environment.</b></span></body>");
    private final ComboBox<SpringBootObservabilityMode> mySpringBootObservabilityModeComboBox = new ComboBox<>(new EnumComboBoxModel<>(SpringBootObservabilityMode.class));
    private final JBTextField myRuntimeObservabilityBackendUrlText = new JBTextField();

    public SettingsComponent() {

        var defaultLabelForeground = JBColor.foreground();

        var myUrlLabel = new JBLabel("Digma API URL: ");
        myApiUrlText.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    var url = new URL(myApiUrlText.getText().trim());
                    if (Objects.equals(url.getProtocol(), "http")) {
                        myUrlLabel.setForeground(JBColor.RED);
                        return false;
                    }
                    myUrlLabel.setForeground(defaultLabelForeground);
                    return true;
                } catch (MalformedURLException e) {
                    myUrlLabel.setForeground(JBColor.RED);
                    return false;
                }
            }
        });

        var myRefreshLabel = new JBLabel("Refresh every (sec.): ");
        myRefreshDelay.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    Integer.parseInt(myRefreshDelay.getText().trim());
                    myRefreshLabel.setForeground(defaultLabelForeground);
                    return true;
                } catch (NumberFormatException e) {
                    myRefreshLabel.setForeground(JBColor.RED);
                    return false;
                }
            }
        });


        myJaegerUrlText.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                if (myJaegerUrlText.getText().isBlank()) {
                    myJaegerUrlLabel.setForeground(defaultLabelForeground);
                    return true;
                }
                try {
                    new URL(myJaegerUrlText.getText().trim());
                    myJaegerUrlLabel.setForeground(defaultLabelForeground);
                    return true;
                } catch (MalformedURLException e) {
                    myJaegerUrlLabel.setForeground(JBColor.RED);
                    return false;
                }
            }
        });


        myJaegerQueryUrlText.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                if (myJaegerQueryUrlText.getText().isBlank()) {
                    myJaegerQueryUrlLabel.setForeground(defaultLabelForeground);
                    return true;
                }
                try {
                    new URL(myJaegerQueryUrlText.getText().trim());
                    myJaegerQueryUrlLabel.setForeground(defaultLabelForeground);
                    return true;
                } catch (MalformedURLException e) {
                    myJaegerQueryUrlLabel.setForeground(JBColor.RED);
                    return false;
                }
            }
        });


        myEmbeddedJaegerMessage.setForeground(JBColor.BLUE);

        var myJaegerLinkModeLabel = new JBLabel("Jaeger link mode: ");
        myJaegerLinkModeLabel.setToolTipText("Internal will open the link as an embedded URL within the IDE. "
                + "External will open the link externally to your default browser. "
                + "Embedded mode will open embedded Jaeger UI in the editor area. "
        );
        myJaegerLinkModeComboBox.addItemListener(e -> {
            LinkMode selected = (LinkMode) myJaegerLinkModeComboBox.getSelectedItem();
            linkModeSelected(defaultLabelForeground, selected);
        });

        var mySpringBootObservabilityModeLabel = new JBLabel("Spring Boot observability mode: ");
        mySpringBootObservabilityModeLabel.setToolTipText("OtelAgent will use the default OpenTelemetry java agent. "
                + "Micrometer will use Micrometer tracing, including the annotation of 'Observed' "
        );

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
                .addLabeledComponent(myJaegerLinkModeLabel, myJaegerLinkModeComboBox, 1, false)
                .addComponent(myEmbeddedJaegerMessage, 1)
                .addLabeledComponent(myJaegerUrlLabel, myJaegerUrlText, 1, false)
                .addLabeledComponent(myJaegerQueryUrlLabel, myJaegerQueryUrlText, 1, false)
                .addLabeledComponent(mySpringBootObservabilityModeLabel, mySpringBootObservabilityModeComboBox, 1, false)
                .addLabeledComponent(myRuntimeObservabilityBackendUrlLabel, myRuntimeObservabilityBackendUrlText, 1, false)
                .addComponentToRightColumn(feedbackForRuntimeObservabilityBackendUrl, 1)
                .addComponent(resetButton)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }


    private void linkModeSelected(Color defaultLabelForeground, LinkMode selected) {
        switch (Objects.requireNonNull(selected)) {
            case External, Internal -> {
                myJaegerQueryUrlLabel.setEnabled(false);
                myJaegerQueryUrlLabel.setForeground(JBColor.GRAY);
                myJaegerQueryUrlText.setEnabled(false);
                myEmbeddedJaegerMessage.setVisible(false);
                myJaegerUrlText.setEnabled(true);
                myJaegerUrlLabel.setEnabled(true);
                myJaegerUrlLabel.setForeground(defaultLabelForeground);
            }
            case Embedded -> {
                myJaegerUrlText.setEnabled(false);
                myJaegerUrlLabel.setEnabled(false);
                myJaegerUrlLabel.setForeground(JBColor.GRAY);
                myJaegerQueryUrlLabel.setEnabled(true);
                myJaegerQueryUrlLabel.setForeground(defaultLabelForeground);
                myJaegerQueryUrlText.setEnabled(true);
                myEmbeddedJaegerMessage.setVisible(true);
            }
        }
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

    public String getJaegerQueryUrl() {
        return myJaegerQueryUrlText.getText().trim();
    }

    public void setJaegerUrl(@Nullable String newText) {
        myJaegerUrlText.setText(newText == null ? "" : newText);
    }

    public void setJaegerQueryUrl(String newText) {
        myJaegerQueryUrlText.setText(newText.trim());
    }

    public LinkMode getJaegerLinkMode() {
        return (LinkMode) myJaegerLinkModeComboBox.getSelectedItem();
    }

    public void setJaegerLinkMode(LinkMode linkMode) {
        //for some reason the combo doesn't always fire an event here so need to emulate it
        myJaegerLinkModeComboBox.setSelectedItem(linkMode);
        linkModeSelected(JBColor.foreground(), linkMode);
    }

    public SpringBootObservabilityMode getSpringBootObservabilityMode() {
        return (SpringBootObservabilityMode) mySpringBootObservabilityModeComboBox.getSelectedItem();
    }

    public void setSpringBootObservabilityMode(SpringBootObservabilityMode mode) {
        mySpringBootObservabilityModeComboBox.setSelectedItem(mode);
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
        this.setJaegerUrl("");
        this.setJaegerQueryUrl(SettingsState.DEFAULT_JAEGER_QUERY_URL);
        this.setJaegerLinkMode(SettingsState.DEFAULT_JAEGER_LINK_MODE);
        this.myEmbeddedJaegerMessage.setVisible(true);
        this.setSpringBootObservabilityMode(SettingsState.DEFAULT_SPRING_BOOT_OBSERVABILITY_MODE);
        this.setRuntimeObservabilityBackendUrl(SettingsState.DEFAULT_RUNTIME_OBSERVABILITY_BACKEND_URL);
    }
}
