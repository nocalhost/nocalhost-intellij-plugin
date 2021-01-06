package dev.nocalhost.plugin.intellij.ui;

import com.google.inject.Inject;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import java.awt.*;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.ui.action.RefreshAction;
import dev.nocalhost.plugin.intellij.ui.action.SettingsAction;
import dev.nocalhost.plugin.intellij.utils.CommonUtils;

public class NocalhostWindow {

    @Inject
    private SettingsAction settingsAction;
    @Inject
    private RefreshAction refreshAction;
    @Inject
    private NocalhostSettings nocalhostSettings;
    @Inject
    private CommonUtils commonUtils;
    @Inject
    private Logger log;

    public SimpleToolWindowPanel createToolWindowContent(final Project project) {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        ActionToolbar toolbar = createToolbar(project);
        panel.setToolbar(toolbar.getComponent());
        JLabel label = new JLabel();
        UserInfo userInfo = nocalhostSettings.getUser();
        if (userInfo == null) {
            label.setText("Nocalhost");
        } else {
            label.setText(userInfo.getEmail());
        }
        panel.add(label, BorderLayout.NORTH);

        JButton loginButton = new JButton("Login");
        panel.add(loginButton, BorderLayout.SOUTH);
        loginButton.addActionListener(e -> {
            final LoginDialog dialog = new LoginDialog(project, nocalhostSettings, commonUtils, log);
            dialog.show();
            if (dialog.isOK()) {
                UserInfo u = nocalhostSettings.getUser();
                JLabel nameLabel = new JLabel();
                if (u != null) {
                    nameLabel.setText(u.getEmail());
                    panel.add(nameLabel, BorderLayout.NORTH);
                    panel.remove(label);
                    JButton logoutButton = new JButton("Logout");
                    panel.add(logoutButton, BorderLayout.SOUTH);
                    logoutButton.addActionListener(e1 -> {
                        nocalhostSettings.clearAuth();
                    });
                }
            }
        });
        panel.setVisible(true);
        return panel;
    }

    private ActionToolbar createToolbar(final Project project) {
        DefaultActionGroup groupFromConfig = (DefaultActionGroup) ActionManager.getInstance().getAction("Nocalhost.Toolbar");
//        DefaultActionGroup group = new DefaultActionGroup(groupFromConfig); // copy required (otherwise config action group gets modified)
//        group.add(refreshAction);
//        group.add(settingsAction);
        return ActionManager.getInstance().createActionToolbar("Nocalhost.Toolbar", groupFromConfig, true);
    }
}
