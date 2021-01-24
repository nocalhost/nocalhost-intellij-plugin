package dev.nocalhost.plugin.intellij.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
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
    private JBTextField specifyOneTextField;
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
            specifyOneTextField.setEnabled(specifyOneRadioButton.isSelected());
            if (specifyOneRadioButton.isSelected()) {
                specifyOneTextField.grabFocus();
            }
        });

        specifyOneTextField.setEnabled(false);
        defaultRadioButton.setSelected(true);

        if (StringUtils.equals(devSpace.getContext().getInstallType(), "helmRepo")) {
            messageLabel.setText("Which version to install?");
            defaultRadioButton.setText("Default Version");
            specifyOneTextField.getEmptyText().appendText("input the version of chart");
        } else {
            messageLabel.setText("Which branch to install(Manifests in Git Repo)?");
            defaultRadioButton.setText("Default Branch");
            specifyOneTextField.getEmptyText().appendText("input the branch of repository");
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (specifyOneRadioButton.isSelected() && !StringUtils.isNotEmpty(specifyOneTextField.getText())) {
            if (StringUtils.equals(devSpace.getContext().getInstallType(), "helmRepo")) {
                return new ValidationInfo("Chart version cannot be empty", specifyOneTextField);
            } else {
                return new ValidationInfo("Git ref cannot be empty", specifyOneTextField);
            }
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        DevSpace.Context context = devSpace.getContext();

        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        final NhctlInstallOptions opts = new NhctlInstallOptions();

        final String installType = devSpace.getContext().getInstallType();
        if (StringUtils.equals(installType, "helmGit") || StringUtils.equals(installType, "helmRepo")
                && MessageDialogBuilder.yesNo("Do you want to specify a values.yaml?", "")
                .yesText("Specify One").noText("Use Default values").guessWindowAndAsk()) {
            final FileChooserDescriptor valueFileChooser = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
            valueFileChooser.setShowFileSystemRoots(true);
            FileChooser.chooseFiles(valueFileChooser, null, null, paths -> {
                Path valueFilePath = paths.get(0).toNioPath();
                opts.setHelmValues(valueFilePath.toString());
            });
        }

        opts.setGitUrl(context.getApplicationUrl());
        opts.setType(NhctlHelper.generateInstallType(context.getSource(), context.getInstallType()));
        opts.setResourcesPath(Arrays.asList(context.getResourceDir()));
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());
        opts.setNamespace(devSpace.getNamespace());

        if (specifyOneRadioButton.isSelected()) {
            if (StringUtils.equals(installType, "helmRepo")) {
                opts.setHelmRepoUrl(context.getApplicationUrl());
                opts.setHelmChartName(context.getApplicationName());
                opts.setHelmRepoVersion(specifyOneTextField.getText());
            } else {
                opts.setGitRef(specifyOneTextField.getText());
            }
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

                    Notifications.Bus.notify(new Notification(
                            "Nocalhost.Notification",
                            "Application " + context.getApplicationName() + " installed",
                            "",
                            NotificationType.INFORMATION));
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        super.doOKAction();
    }
}
