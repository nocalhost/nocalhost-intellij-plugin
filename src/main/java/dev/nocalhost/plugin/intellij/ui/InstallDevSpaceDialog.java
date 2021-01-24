package dev.nocalhost.plugin.intellij.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class InstallDevSpaceDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JLabel messageLabel;
    private JRadioButton defaultRadioButton;
    private JRadioButton specifyOneRadioButton;
    private JTextField gitRefField;
    private DevSpace devSpace;

    public InstallDevSpaceDialog(DevSpace devSpace) {
        super(true);
        init();
        setTitle("Install DevSpace: " + devSpace.getSpaceName());

        this.devSpace = devSpace;

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultRadioButton);
        buttonGroup.add(specifyOneRadioButton);

        specifyOneRadioButton.addChangeListener(e -> {
            gitRefField.setEnabled(specifyOneRadioButton.isSelected());
            if (specifyOneRadioButton.isSelected()) {
                gitRefField.grabFocus();
            }
        });

        gitRefField.setEnabled(false);
        defaultRadioButton.setSelected(true);

        if (StringUtils.equals(devSpace.getContext().getInstallType(), "helmRepo")) {
            messageLabel.setText("Which version to install?");
            defaultRadioButton.setText("Default Version");
        } else {
            messageLabel.setText("Which branch to install(Manifests in Git Repo)?");
            defaultRadioButton.setText("Default Branch");
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (specifyOneRadioButton.isSelected() && !StringUtils.isNotEmpty(gitRefField.getText())) {
            return new ValidationInfo("Git ref cannot be empty", gitRefField);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        DevSpace.Context context = devSpace.getContext();

        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlInstallOptions opts = new NhctlInstallOptions();
        opts.setGitUrl(context.getApplicationUrl());
        opts.setType(NhctlHelper.generateInstallType(context.getSource(), context.getInstallType()));
        opts.setResourcesPath(Arrays.asList(context.getResourceDir()));
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());
        opts.setNamespace(devSpace.getNamespace());

        if (specifyOneRadioButton.isSelected()) {
            opts.setGitRef(gitRefField.getText());
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Installing application: " + context.getApplicationName(), false) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    nhctlCommand.install(context.getApplicationName(), opts);

                    final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                    nocalhostApi.syncInstallStatus(devSpace, 1);

                    final Application application = ApplicationManager.getApplication();
                    DevSpaceListUpdatedNotifier publisher = application.getMessageBus()
                            .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
                    publisher.action();

                    Notifications.Bus.notify(new Notification("Nocalhost.Notification", "Application " + context.getApplicationName() + " installed", "", NotificationType.INFORMATION));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        super.doOKAction();
    }
}
