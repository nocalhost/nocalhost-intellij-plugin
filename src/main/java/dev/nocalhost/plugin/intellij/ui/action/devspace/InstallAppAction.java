package dev.nocalhost.plugin.intellij.ui.action.devspace;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceOption;
import dev.nocalhost.plugin.intellij.ui.InstallDevSpaceOptionDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.HelmNocalhostConfigUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class InstallAppAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(InstallAppAction.class);
    private static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

    private final Project project;
    private final DevSpaceNode node;

    public InstallAppAction(Project project, DevSpaceNode node) {
        super("Install App", "", AllIcons.Actions.Install);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();

        try {
            if (NhctlHelper.isApplicationInstalled(devSpace)) {
                Messages.showMessageDialog("Application has been installed.", "Install application", null);
                return;
            }
            installApp();
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while checking if application was installed", e);
            return;
        }
    }

    private void installApp() throws IOException {
        final DevSpace devSpace = node.getDevSpace();
        final DevSpace.Context context = devSpace.getContext();
        final String installType = NhctlHelper.generateInstallType(devSpace.getContext());

        final NhctlInstallOptions opts = new NhctlInstallOptions();
        opts.setType(installType);
        opts.setResourcesPath(Arrays.asList(context.getResourceDir()));
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());
        opts.setNamespace(devSpace.getNamespace());

        if (Set.of("helmLocal", "rawManifestLocal").contains(installType)) {
            String message = StringUtils.equals(installType, "rawManifestLocal")
                    ? "Please choose application manifest root directory"
                    : "Please choose unpacked application helm chart root directory";

            Path localPath = FileChooseUtil.chooseSingleDirectory(project, message);
            if (localPath == null) {
                return;
            }

            Path configPath = null;
            List<Path> configs = getAllConfig(localPath.resolve(".nocalhost"));
            if (configs.size() == 0) {
                configPath = localPath;
            } else if (configs.size() == 1) {
                configPath = configs.get(0);
            } else if (configs.size() > 1) {
                configPath = FileChooseUtil.chooseSingleFile(project, "Please select your configuration file", localPath.resolve(".nocalhost"), CONFIG_FILE_EXTENSIONS);
            }
            if (configPath == null) {
                return;
            }

            opts.setLocalPath(localPath.toString());
            opts.setOuterConfig(configPath.toString());

        } else {
            InstallDevSpaceOption installDevSpaceOption = askAndGetInstallOption(installType);
            if (installDevSpaceOption == null) {
                return;
            }
            if (installDevSpaceOption.isSpecifyOneSelected()) {
                if (StringUtils.equals(installType, "helmRepo")) {
                    opts.setHelmRepoVersion(installDevSpaceOption.getSpecifyText());
                } else {
                    opts.setGitRef(installDevSpaceOption.getSpecifyText());
                }
            }

            if (StringUtils.equals(installType, "helmRepo")) {
                opts.setHelmRepoUrl(context.getApplicationUrl());
                opts.setHelmChartName(context.getApplicationName());
                opts.setOuterConfig(HelmNocalhostConfigUtil.helmNocalhostConfigPath(devSpace).toString());
            } else {
                opts.setGitUrl(context.getApplicationUrl());
            }
        }

        if (Set.of("helmGit", "helmRepo", "helmLocal").contains(installType)) {
            HelmValuesChooseDialog helmValuesChooseDialog = new HelmValuesChooseDialog(project);
            if (helmValuesChooseDialog.showAndGet()) {
                HelmValuesChooseState helmValuesChooseState = helmValuesChooseDialog.getHelmValuesChooseState();
                if (helmValuesChooseState.isSpecifyValuesYamlSelected()) {
                    opts.setHelmValues(helmValuesChooseState.getValuesYamlPath());
                }
                if (helmValuesChooseState.isSpecifyValues() && Set.of("helmGit", "helmRepo").contains(installType)) {
                    opts.setValues(helmValuesChooseState.getValues());
                }
            } else {
                return;
            }
        }

        if (Set.of("rawManifest", "helmGit").contains(installType)) {
            opts.setConfig(context.getApplicationConfigPath());
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
                } catch (IOException | InterruptedException | NocalhostExecuteCmdException | NocalhostApiException e) {
                    LOG.error("error occurred while installing application", e);
                    NocalhostNotifier.getInstance(project).notifyError("Nocalhost install devSpace error", "Error occurred while installing application", e.getMessage());
                }
            }
        });
    }

    private InstallDevSpaceOption askAndGetInstallOption(String installType) {
        InstallDevSpaceOptionDialog dialog;
        if (StringUtils.equals(installType, "helmRepo")) {
            dialog = new InstallDevSpaceOptionDialog(
                    project,
                    node.getDevSpace(),
                    "Which version to install?",
                    "Default Version",
                    "Input the version of chart",
                    "Chart version cannot be empty");
        } else {
            dialog = new InstallDevSpaceOptionDialog(
                    project,
                    node.getDevSpace(),
                    "Which branch to install(Manifests in Git Repo)?",
                    "Default Branch",
                    "Input the branch of repository",
                    "Git ref cannot be empty");
        }

        if (!dialog.showAndGet()) {
            return null;
        }

        return dialog.getInstallDevSpaceOption();
    }

    private List<Path> getAllConfig(Path localPath) throws IOException {
        if (Files.notExists(localPath)) {
            return Lists.newArrayList();
        }

        return Files.list(localPath)
                .filter(Files::isRegularFile)
                .filter(e -> CONFIG_FILE_EXTENSIONS.contains(com.google.common.io.Files.getFileExtension(e.getFileName().toString())))
                .map(Path::toAbsolutePath)
                .collect(Collectors.toList());
    }
}
