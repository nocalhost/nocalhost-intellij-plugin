package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.Lists;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.ui.components.JBScrollPane;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputActivateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.NocalhostTree;

public class NocalhostWindow {

    private final Project project;
    private final ToolWindow toolWindow;

    private JPanel panel;
    private NocalhostTree tree;
    private JBScrollPane scrollPane;
    private final JButton loginButton;

    public NocalhostWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect().subscribe(
                NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC,
                NocalhostWindow.this::toggleContent
        );
        application.getMessageBus().connect().subscribe(
                DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC,
                NocalhostWindow.this::updateTree
        );

        project.getMessageBus().connect().subscribe(
                NocalhostOutputActivateNotifier.NOCALHOST_OUTPUT_ACTIVATE_NOTIFIER,
                this::activateOutput
        );

        devStart();

        panel = new SimpleToolWindowPanel(true, true);
        loginButton = new JButton("Login");
        tree = new NocalhostTree(project);
        scrollPane = new JBScrollPane(tree);
        panel.add(scrollPane);
        panel.add(loginButton, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> {
            new LoginDialog().showAndGet();
        });

        toggleContent();
    }

    private void toggleContent() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String jwt = nocalhostSettings.getJwt();
        if (StringUtils.isNotBlank(jwt)) {
            toolWindow.setTitleActions(Lists.newArrayList(
                    ActionManager.getInstance().getAction("Nocalhost.RefreshAction"),
                    ActionManager.getInstance().getAction("Nocalhost.LogoutAction")
            ));
            tree.clear();
            tree.updateDevSpaces();
            loginButton.setVisible(false);
            scrollPane.setVisible(true);
        } else {
            toolWindow.setTitleActions(Lists.newArrayList());
            scrollPane.setVisible(false);
            loginButton.setVisible(true);
        }
    }

    private void updateTree() {
        tree.updateDevSpaces();
    }

    private void devStart() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        DevModeService devModeService = nocalhostSettings.getDevModeProjectBasePath2Service().get(project.getBasePath());
        if (nocalhostSettings.getUserInfo() != null && devModeService != null) {
            final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
            try {
                for (DevSpace devSpace : nocalhostApi.listDevSpace()) {
                    if (devSpace.getId() == devModeService.getApplicationId()
                            && devSpace.getDevSpaceId() == devModeService.getDevSpaceId()) {
                        ProgressManager.getInstance().run(new StartingDevModeTask(project, devSpace, devModeService));
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                nocalhostSettings.getDevModeProjectBasePath2Service().remove(project.getBasePath());
            }
        }
    }

    private void activateOutput() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Output").activate(() -> {
                });
            } catch (AlreadyDisposedException e) {
                // Ignore
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }
}
