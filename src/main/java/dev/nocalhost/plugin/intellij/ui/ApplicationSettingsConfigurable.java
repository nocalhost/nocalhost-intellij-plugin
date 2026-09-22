package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostI18nChangedNotifier;

public class ApplicationSettingsConfigurable implements SearchableConfigurable {
    private static final String[] LANGUAGES = {NocalhostI18n.LANG_EN, NocalhostI18n.LANG_ZH};

    private static NocalhostSettings settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
    private JPanel panel;
    private JCheckBox checkNhctlVersion;
    private JLabel checkNhctlVersionDescription;
    private JComboBox<String> languageComboBox;
    private JLabel languageLabel;
    private JCheckBox autoReconnectDebug;
    private JLabel autoReconnectDebugDescription;
    private JLabel nhctlVersionOverrideLabel;
    private JTextField nhctlVersionOverride;
    private JLabel nhctlVersionOverrideDescription;

    public ApplicationSettingsConfigurable() {
        checkNhctlVersion.setSelected(settings.getCheckNhctlVersion());
        String current = NocalhostI18n.getLanguage();
        languageComboBox.setSelectedIndex(current.equals(NocalhostI18n.LANG_ZH) ? 1 : 0);
        // Keep the combo itself localized so it always offers the two supported languages.
        languageLabel.setText(NocalhostI18n.get("settings.language"));
        checkNhctlVersion.setText(NocalhostI18n.get("settings.checkNhctlVersion"));
        checkNhctlVersionDescription.setText(NocalhostI18n.get("settings.checkNhctlVersion.description"));
        autoReconnectDebug.setSelected(settings.getAutoReconnectDebug());
        autoReconnectDebug.setText(NocalhostI18n.get("settings.autoReconnectDebug"));
        autoReconnectDebugDescription.setText(NocalhostI18n.get("settings.autoReconnectDebug.description"));
        nhctlVersionOverrideLabel.setText(NocalhostI18n.get("settings.nhctlVersionOverride"));
        nhctlVersionOverride.setText(settings.getNhctlVersionOverride());
        nhctlVersionOverrideDescription.setText(NocalhostI18n.get("settings.nhctlVersionOverride.description"));
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "dev.nocalhost.plugin.intellij.ui.ApplicationSettingsConfigurable";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Nocalhost";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return panel;
    }

    @Override
    public boolean isModified() {
        return settings.getCheckNhctlVersion() != checkNhctlVersion.isSelected()
                || settings.getAutoReconnectDebug() != autoReconnectDebug.isSelected()
                || !settings.getNhctlVersionOverride().equals(nhctlVersionOverride.getText().trim())
                || !LANGUAGES[languageComboBox.getSelectedIndex()].equals(settings.getLanguage());
    }

    @Override
    public void apply() throws ConfigurationException {
        settings.setCheckNhctlVersion(checkNhctlVersion.isSelected());
        settings.setAutoReconnectDebug(autoReconnectDebug.isSelected());
        settings.setNhctlVersionOverride(nhctlVersionOverride.getText());
        String language = LANGUAGES[languageComboBox.getSelectedIndex()];
        if (!language.equals(settings.getLanguage())) {
            settings.setLanguage(language);
            // Invalidate i18n cache so the new language takes effect immediately.
            NocalhostI18n.refresh();
            // Notify the tool window / status bar to re-render right away.
            ApplicationManager.getApplication().getMessageBus().syncPublisher(
                    NocalhostI18nChangedNotifier.NOCALHOST_I18N_CHANGED_NOTIFIER_TOPIC).action();
        }
    }

    @Override
    public void reset() {
        checkNhctlVersion.setSelected(settings.getCheckNhctlVersion());
        languageComboBox.setSelectedIndex(
                NocalhostI18n.LANG_ZH.equals(settings.getLanguage()) ? 1 : 0);
        autoReconnectDebug.setSelected(settings.getAutoReconnectDebug());
        nhctlVersionOverride.setText(settings.getNhctlVersionOverride());
    }
}
