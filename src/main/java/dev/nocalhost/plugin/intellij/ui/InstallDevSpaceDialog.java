package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.utils.HelmNocalhostConfigUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class InstallDevSpaceDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(InstallDevSpaceDialog.class);

    private final Project project;

    private JPanel dialogPanel;
    private JLabel messageLabel;
    private JRadioButton defaultRadioButton;
    private JRadioButton specifyOneRadioButton;
    private JBTextField specifyOneTextField;
    private DevSpace devSpace;

    public InstallDevSpaceDialog(Project project, DevSpace devSpace) {
        super(true);
        init();
        setTitle("Install DevSpace: " + devSpace.getSpaceName());

        this.project = project;
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

        final String nhctlInstallType = NhctlHelper.generateInstallType(devSpace.getContext());
        if (StringUtils.equals(nhctlInstallType, "helmRepo")) {
            messageLabel.setText("Which version to install?");
            defaultRadioButton.setText("Default Version");
            specifyOneTextField.getEmptyText().appendText("Input the version of chart");
        } else {
            messageLabel.setText("Which branch to install(Manifests in Git Repo)?");
            defaultRadioButton.setText("Default Branch");
            specifyOneTextField.getEmptyText().appendText("Input the branch of repository");
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        final String nhctlInstallType = NhctlHelper.generateInstallType(devSpace.getContext());
        if (specifyOneRadioButton.isSelected() && !StringUtils.isNotEmpty(specifyOneTextField.getText())) {
            if (StringUtils.equals(nhctlInstallType, "helmRepo")) {
                return new ValidationInfo("Chart version cannot be empty", specifyOneTextField);
            } else {
                return new ValidationInfo("Git ref cannot be empty", specifyOneTextField);
            }
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();

        ApplicationManager.getApplication().invokeLater(() -> {
            final DevSpace.Context context = devSpace.getContext();
            final String nhctlInstallType = NhctlHelper.generateInstallType(context);

            final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

            final NhctlInstallOptions opts = new NhctlInstallOptions();

            if (StringUtils.equals(nhctlInstallType, "helmGit") || StringUtils.equals(nhctlInstallType, "helmRepo")) {
                int specifyValues = MessageDialogBuilder
                        .yesNoCancel("Install application", "Do you want to specify a values.yaml?")
                        .yesText("Specify One")
                        .noText("Use Default values")
                        .guessWindowAndAsk();
                if (specifyValues == Messages.CANCEL) {
                    return;
                }
                if (specifyValues == Messages.YES) {
                    final List<Path> chosenFiles = Lists.newArrayList();

                    final FileChooserDescriptor valueFileChooser = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
                    valueFileChooser.setShowFileSystemRoots(true);
                    FileChooser.chooseFiles(valueFileChooser, null, null, paths -> {
                        paths.stream().forEach((p) -> chosenFiles.add(p.toNioPath()));
                    });

                    if (chosenFiles.size() <= 0) {
                        return;
                    }
                    opts.setHelmValues(chosenFiles.get(0).toString());
                }
            }
            if (StringUtils.equals(nhctlInstallType, "rawManifest")) {
                if (StringUtils.isNotEmpty(context.getApplicationConfigPath())) {
                    opts.setConfig(context.getApplicationConfigPath());
                }
            }

            opts.setType(nhctlInstallType);
            opts.setResourcesPath(Arrays.asList(context.getResourceDir()));
            opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());
            opts.setNamespace(devSpace.getNamespace());

            if (StringUtils.equals(nhctlInstallType, "helmRepo")) {
                opts.setHelmRepoUrl(context.getApplicationUrl());
                opts.setHelmChartName(context.getApplicationName());
                opts.setOuterConfig(HelmNocalhostConfigUtil.helmNocalhostConfigPath(devSpace).toString());
            } else {
                opts.setGitUrl(context.getApplicationUrl());
            }

            if (specifyOneRadioButton.isSelected()) {
                if (StringUtils.equals(nhctlInstallType, "helmRepo")) {
                    opts.setHelmRepoVersion(specifyOneTextField.getText());
                } else {
                    opts.setGitRef(specifyOneTextField.getText());
                }
            }

            ProgressManager.getInstance().run(new Task.Backgroundable(null, "Installing application: " + context.getApplicationName(), false) {

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
                        outputCapturedNhctlCommand.install(context.getApplicationName(), opts);

                        final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                        nocalhostApi.syncInstallStatus(devSpace, 1);

                        final Application application = ApplicationManager.getApplication();
                        DevSpaceListUpdatedNotifier publisher = application.getMessageBus()
                                .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
                        publisher.action();

                        NocalhostNotifier.getInstance(project).notifySuccess("Application " + context.getApplicationName() + " installed", "");
                    } catch (IOException | InterruptedException e) {
                        LOG.error("error occurred while installing application", e);
                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost install devSpace error", "Error occurred while installing application", e.getMessage());
                    }
                }
            });
        });
    }
}
