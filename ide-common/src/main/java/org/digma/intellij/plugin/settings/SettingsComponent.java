package org.digma.intellij.plugin.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.*;
import com.intellij.ui.components.*;
import com.intellij.ui.components.fields.*;
import com.intellij.util.ui.FormBuilder;
import org.digma.intellij.plugin.analytics.BackendInfoHolder;
import org.digma.intellij.plugin.auth.account.*;
import org.digma.intellij.plugin.common.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.List;
import java.util.*;

import static org.digma.intellij.plugin.settings.SettingsState.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
class SettingsComponent {


    private static final List<String> ALLOW_HTTPS = Collections.singletonList("https");
    private static final List<String> ALLOW_HTTP_AND_HTTPS = Arrays.asList("http", "https");


    private final JPanel myMainPanel;
    private final JBTextField myApiUrlTextField = new JBTextField();
    private final JBTextField myApiTokenTestField = new JBTextField();
    private final IntegerField myRefreshDelayTextField = new IntegerField();
    private final JBTextField myJaegerUrlTextField = new JBTextField();
    private final JBLabel myJaegerUrlLabel = new JBLabel("Jaeger URL: (For internal/external mode)");
    private final JBTextField myJaegerQueryUrlTextField = new JBTextField();
    private final JBLabel myJaegerQueryUrlLabel = new JBLabel("Jaeger Query URL (For embedded mode): ");
    private final ComboBox<JaegerLinkMode> myJaegerLinkModeComboBox = new ComboBox<>(new EnumComboBoxModel<>(JaegerLinkMode.class));
    private final JBLabel myEmbeddedJaegerMessage = new JBLabel("<html><body><span style=\"color:\"" + JBColor.BLUE + "\"\"><b>Jaeger embedded is only supported for deployment on a local environment.</b></span></body>");
    private final ComboBox<SpringBootObservabilityMode> mySpringBootObservabilityModeComboBox = new ComboBox<>(new EnumComboBoxModel<>(SpringBootObservabilityMode.class));
    private final JBLabel myRuntimeObservabilityBackendUrlLabel = new JBLabel("Runtime observability backend URL:");
    private final JBTextField myRuntimeObservabilityBackendUrlTextField = new JBTextField();
    private final ExpandableTextField extendedObservabilityTextFiled = new ExpandableTextField();
    private final ExpandableTextField extendedObservabilityExcludeTextField = new ExpandableTextField();

    public SettingsComponent() {

        myRefreshDelayTextField.setMinValue(10);
        myRefreshDelayTextField.setMaxValue(100);


        extendedObservabilityTextFiled.setToolTipText("package names in format 'my.pkg1;my.pkg2");
        extendedObservabilityExcludeTextField.setToolTipText("class/method names to exclude in format 'MyClass;MyOtherClass.myOtherMethod;*get");
        var extendedObservabilityTextBoxPanel = new JBPanel<JBPanel<?>>();
        extendedObservabilityTextBoxPanel.setLayout(new BorderLayout());
        extendedObservabilityTextBoxPanel.add(extendedObservabilityTextFiled, BorderLayout.CENTER);
        var extendedObservabilityExcludeTextBoxPanel = new JBPanel<JBPanel<?>>();
        extendedObservabilityExcludeTextBoxPanel.setLayout(new BorderLayout());
        extendedObservabilityExcludeTextBoxPanel.add(extendedObservabilityExcludeTextField, BorderLayout.CENTER);

        var defaultLabelForeground = JBColor.foreground();


        var myUrlLabel = new JBLabel("Digma API URL: ");
        myApiUrlTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    URLValidator.create(myApiUrlTextField.getText().trim(), ALLOW_HTTPS).validate();
                    myUrlLabel.setForeground(defaultLabelForeground);
                    myApiUrlTextField.setToolTipText(null);
                    return true;
                } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                         URLValidator.IncorrectSchemaException e) {
                    myUrlLabel.setForeground(JBColor.RED);
                    myApiUrlTextField.setToolTipText(e.getMessage());
                    return false;
                }
            }
        });

        var myRefreshLabel = new JBLabel("Refresh every (sec.): ");
        myRefreshDelayTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    int refreshDelay = Integer.parseInt(myRefreshDelayTextField.getText().trim());
                    if (refreshDelay < MINIMUM_DEFAULT_REFRESH_DELAY) {
                        myRefreshLabel.setForeground(JBColor.RED);
                        myRefreshDelayTextField.setToolTipText("Must not be lower then " + MINIMUM_DEFAULT_REFRESH_DELAY);
                        return false;
                    } else if (refreshDelay > MAXIMUM_DEFAULT_REFRESH_DELAY) {
                        myRefreshLabel.setForeground(JBColor.RED);
                        myRefreshDelayTextField.setToolTipText("Must not be higher then " + MAXIMUM_DEFAULT_REFRESH_DELAY);
                        return false;
                    } else {
                        myRefreshLabel.setForeground(defaultLabelForeground);
                        myRefreshDelayTextField.setToolTipText(null);
                        return true;
                    }
                } catch (NumberFormatException e) {
                    myRefreshLabel.setForeground(JBColor.RED);
                    myRefreshDelayTextField.setToolTipText("Refresh delay is not a number: " + e.getMessage());
                    return false;
                }
            }
        });


        myJaegerUrlTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                if (myJaegerUrlTextField.getText() == null || myJaegerUrlTextField.getText().isBlank()) {
                    myJaegerUrlLabel.setForeground(defaultLabelForeground);
                    myJaegerUrlTextField.setToolTipText(null);
                    return true;
                }
                try {
                    URLValidator.create(myJaegerUrlTextField.getText().trim(), ALLOW_HTTP_AND_HTTPS).validate();
                    myJaegerUrlLabel.setForeground(defaultLabelForeground);
                    return true;
                } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                         URLValidator.IncorrectSchemaException e) {
                    myJaegerUrlLabel.setForeground(JBColor.RED);
                    myJaegerUrlTextField.setToolTipText(e.getMessage());
                    return false;
                }
            }
        });


        myJaegerQueryUrlTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    URLValidator.create(myJaegerQueryUrlTextField.getText().trim(), ALLOW_HTTP_AND_HTTPS).validate();
                    myJaegerQueryUrlLabel.setForeground(defaultLabelForeground);
                    myJaegerQueryUrlTextField.setToolTipText(null);
                    return true;
                } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                         URLValidator.IncorrectSchemaException e) {
                    myJaegerQueryUrlLabel.setForeground(JBColor.RED);
                    myJaegerQueryUrlTextField.setToolTipText(e.getMessage());
                    return false;
                }
            }
        });


        myRuntimeObservabilityBackendUrlLabel.setToolTipText("Where should observability data be sent from the IDE? This would be the Digma collector URL typically listening to port 5050");

        myRuntimeObservabilityBackendUrlTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    URLValidator.create(myRuntimeObservabilityBackendUrlTextField.getText().trim(), ALLOW_HTTP_AND_HTTPS).validate();
                    myRuntimeObservabilityBackendUrlLabel.setForeground(defaultLabelForeground);
                    myRuntimeObservabilityBackendUrlTextField.setToolTipText(null);
                    return true;
                } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                         URLValidator.IncorrectSchemaException e) {
                    myRuntimeObservabilityBackendUrlLabel.setForeground(JBColor.RED);
                    myRuntimeObservabilityBackendUrlTextField.setToolTipText(e.getMessage());
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
            JaegerLinkMode selected = (JaegerLinkMode) myJaegerLinkModeComboBox.getSelectedItem();
            linkModeSelected(defaultLabelForeground, selected);
        });

        var mySpringBootObservabilityModeLabel = new JBLabel("Spring Boot observability mode: ");
        mySpringBootObservabilityModeLabel.setToolTipText("OtelAgent will use the default OpenTelemetry java agent. "
                + "Micrometer will use Micrometer tracing, including the annotation of 'Observed' "
        );

        var resetButton = new JButton("Reset to defaults");
        resetButton.addActionListener(e -> resetToDefaults());


        var userId = "";
        if (DigmaDefaultAccountHolder.getInstance().getAccount() != null &&
                !DigmaDefaultAccountHolder.getInstance().getAccount().getUserId().equals(DigmaAccountKt.DEFAULT_LOGIN_ID)) {
            userId = DigmaDefaultAccountHolder.getInstance().getAccount().getUserId();
        }
        var userIdLabel = new JBLabel(userId);
        userIdLabel.setCopyable(true);

        var backendVersionLabel = new JBLabel("Unknown");
        var someProject = ProjectUtilsKt.findActiveProject();
        if (someProject != null) {
            var about = BackendInfoHolder.getInstance(someProject).getAbout();
            if (about != null) {
                backendVersionLabel.setText(about.getApplicationVersion());
            }
        }

        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(myUrlLabel, myApiUrlTextField, 1, false)
                .addLabeledComponent(new JBLabel("Api token:"), myApiTokenTestField, 1, false)
                .addLabeledComponent(myRefreshLabel, myRefreshDelayTextField, 1, false)
                .addLabeledComponent(myJaegerLinkModeLabel, myJaegerLinkModeComboBox, 1, false)
                .addComponent(myEmbeddedJaegerMessage, 1)
                .addLabeledComponent(myJaegerUrlLabel, myJaegerUrlTextField, 1, false)
                .addLabeledComponent(myJaegerQueryUrlLabel, myJaegerQueryUrlTextField, 1, false)
                .addLabeledComponent(mySpringBootObservabilityModeLabel, mySpringBootObservabilityModeComboBox, 1, false)
                .addLabeledComponent(myRuntimeObservabilityBackendUrlLabel, myRuntimeObservabilityBackendUrlTextField, 1, false)
                .addLabeledComponent("Extended observability (beta)", extendedObservabilityTextBoxPanel, 1, false)
                .addLabeledComponent("Extended observability exclude (beta)", extendedObservabilityExcludeTextBoxPanel, 1, false)
                .addComponent(resetButton)
                .addLabeledComponent(new JBLabel("User id"), userIdLabel)
                .addLabeledComponent(new JBLabel("Backend version"), backendVersionLabel)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }


    private void linkModeSelected(Color defaultLabelForeground, JaegerLinkMode selected) {
        switch (Objects.requireNonNull(selected)) {
            case External, Internal -> {
                myJaegerQueryUrlLabel.setEnabled(false);
                myJaegerQueryUrlLabel.setForeground(JBColor.GRAY);
                myJaegerQueryUrlTextField.setEnabled(false);
                myEmbeddedJaegerMessage.setVisible(false);
                myJaegerUrlTextField.setEnabled(true);
                myJaegerUrlLabel.setEnabled(true);
                myJaegerUrlLabel.setForeground(defaultLabelForeground);
            }
            case Embedded -> {
                myJaegerUrlTextField.setEnabled(false);
                myJaegerUrlLabel.setEnabled(false);
                myJaegerUrlLabel.setForeground(JBColor.GRAY);
                myJaegerQueryUrlLabel.setEnabled(true);
                myJaegerQueryUrlLabel.setForeground(defaultLabelForeground);
                myJaegerQueryUrlTextField.setEnabled(true);
                myEmbeddedJaegerMessage.setVisible(true);
            }
        }
    }


    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return myApiUrlTextField;
    }

    @NotNull
    public String getApiUrl() {
        return myApiUrlTextField.getText().trim();
    }

    public void setApiUrl(@NotNull String newText) {
        myApiUrlTextField.setText(newText.trim());
    }

    @Nullable
    public String getApiToken() {
        return myApiTokenTestField.getText() == null || myApiTokenTestField.getText().trim().isBlank() ? null : myApiTokenTestField.getText().trim();
    }

    public void setApiToken(@Nullable String newText) {
        myApiTokenTestField.setText(newText == null ? null : newText.trim());
    }

    @Nullable
    public String getJaegerUrl() {
        return myJaegerUrlTextField.getText() == null || myJaegerUrlTextField.getText().trim().isBlank() ? null : myJaegerUrlTextField.getText().trim();
    }

    public void setJaegerUrl(@Nullable String newText) {
        myJaegerUrlTextField.setText(newText == null ? null : newText.trim());
    }

    @Nullable
    public String getJaegerQueryUrl() {
        return myJaegerQueryUrlTextField.getText() == null || myJaegerQueryUrlTextField.getText().trim().isBlank() ? null : myJaegerQueryUrlTextField.getText().trim();
    }

    public void setJaegerQueryUrl(@Nullable String newText) {
        myJaegerQueryUrlTextField.setText(newText == null ? null : newText.trim());
    }

    @NotNull
    public JaegerLinkMode getJaegerLinkMode() {
        return myJaegerLinkModeComboBox.getSelectedItem() == null ? DEFAULT_JAEGER_LINK_MODE : (JaegerLinkMode) myJaegerLinkModeComboBox.getSelectedItem();
    }

    public void setJaegerLinkMode(@NotNull JaegerLinkMode jaegerLinkMode) {
        //for some reason the combo doesn't always fire an event here so need to emulate it
        myJaegerLinkModeComboBox.setSelectedItem(jaegerLinkMode);
        linkModeSelected(JBColor.foreground(), jaegerLinkMode);
    }

    @NotNull
    public SpringBootObservabilityMode getSpringBootObservabilityMode() {
        return mySpringBootObservabilityModeComboBox.getSelectedItem() == null ? DEFAULT_SPRING_BOOT_OBSERVABILITY_MODE : (SpringBootObservabilityMode) mySpringBootObservabilityModeComboBox.getSelectedItem();
    }

    public void setSpringBootObservabilityMode(@NotNull SpringBootObservabilityMode mode) {
        mySpringBootObservabilityModeComboBox.setSelectedItem(mode);
    }

    @NotNull
    public String getRuntimeObservabilityBackendUrl() {
        return myRuntimeObservabilityBackendUrlTextField.getText().trim();
    }

    public void setRuntimeObservabilityBackendUrl(@NotNull String newText) {
        myRuntimeObservabilityBackendUrlTextField.setText(newText.trim());
    }

    @NotNull
    public String getRefreshDelay() {
        return myRefreshDelayTextField.getText().trim();
    }

    public void setRefreshDelay(@NotNull String newText) {
        myRefreshDelayTextField.setText(newText.trim());
    }

    @Nullable
    public String getExtendedObservability() {
        //remove only the tab character
        return extendedObservabilityTextFiled.getText() == null || extendedObservabilityTextFiled.getText().trim().isBlank()
                ? null : extendedObservabilityTextFiled.getText().trim().replaceAll("\t", "");
    }

    public void setExtendedObservability(@Nullable String extendedObservability) {
        extendedObservabilityTextFiled.setText(extendedObservability);
    }

    @Nullable
    public String getExtendedObservabilityExclude() {
        //remove only the tab character
        return extendedObservabilityExcludeTextField.getText() == null || extendedObservabilityExcludeTextField.getText().trim().isBlank()
                ? null : extendedObservabilityExcludeTextField.getText().trim().replaceAll("\t", "");
    }

    public void setExtendedObservabilityExclude(@Nullable String extendedObservabilityExclude) {
        extendedObservabilityExcludeTextField.setText(extendedObservabilityExclude);
    }


    private void resetToDefaults() {
        //create a new SettingsState object and use it for default startup values
        SettingsState settingsState = new SettingsState();
        this.setApiUrl(settingsState.getApiUrl());
        this.setApiToken(settingsState.getApiToken());
        this.setRefreshDelay(String.valueOf(settingsState.getRefreshDelay()));
        this.setJaegerUrl(settingsState.getJaegerUrl());
        this.setJaegerQueryUrl(settingsState.getJaegerQueryUrl());
        this.setJaegerLinkMode(settingsState.getJaegerLinkMode());
        this.setSpringBootObservabilityMode(settingsState.getSpringBootObservabilityMode());
        this.setRuntimeObservabilityBackendUrl(settingsState.getRuntimeObservabilityBackendUrl());
        this.setExtendedObservability(settingsState.getExtendedObservability());
        this.setExtendedObservabilityExclude(settingsState.getExtendedObservabilityExcludes());

        this.myEmbeddedJaegerMessage.setVisible(true);
    }
}
