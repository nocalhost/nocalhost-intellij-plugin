package dev.nocalhost.plugin.intellij.ui.action.application;

import com.google.common.collect.Lists;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUpgradeOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.exception.NocalhostServerVersionOutDatedException;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.AppInstallOrUpgradeOption;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.dialog.AppInstallOrUpgradeOptionDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.KustomizePathDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class UpgradeAppAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(UpgradeAppAction.class);
    private static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

    private final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);

    private final Project project;
    private final ApplicationNode node;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;

    public UpgradeAppAction(Project project, ApplicationNode node) {
        super("Upgrade Application");
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        this.applicationName = node.getName();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NocalhostAccount nocalhostAccount = node.getClusterNode().getNocalhostAccount();
                List<Application> nocalhostApplications = nocalhostApi.listApplications(
                        nocalhostAccount.getServer(),
                        nocalhostAccount.getJwt(),
                        nocalhostAccount.getUserInfo().getId()
                );
                Optional<Application> applicationOptional = nocalhostApplications.stream()
                        .filter(e -> StringUtils.equals(
                                applicationName,
                                e.getContext().getApplicationName()
                        ))
                        .findFirst();
                if (applicationOptional.isPresent()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            upgradeApp(applicationOptional.get());
                        } catch (IOException e) {
                            LOG.error("error occurred while upgrading application", e);
                        }
                    });
                } else {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NocalhostNotifier.getInstance(project).notifyError(
                                "Application '" + applicationName + "' not found", "");
                    });
                }

            } catch (NocalhostServerVersionOutDatedException e) {
                NocalhostNotifier.getInstance(project).notifyError("Server version out-dated", e.getMessage());
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading application status error",
                        "Error occurs while loading application status", e);
            }
        });
    }

    private void upgradeApp(Application application) throws IOException {
        final Application.Context context = application.getContext();
        final String installType = application.getApplicationType();

        final NhctlUpgradeOptions opts = new NhctlUpgradeOptions(kubeConfigPath, namespace);
        List<String> resourceDirs = Lists.newArrayList(context.getResourceDir());

        if (Set.of("helmLocal", "rawManifestLocal").contains(installType)) {
            String message = StringUtils.equals(installType, "rawManifestLocal")
                    ? "Please choose application manifest root directory"
                    : "Please choose unpacked application helm chart root directory";

            Path localPath = FileChooseUtil.chooseSingleDirectory(project, "", message);
            if (localPath == null) {
                return;
            }

            Path configPath = null;
            Path nocalhostConfigPath = localPath.resolve(".nocalhost");
            List<Path> configs = getAllConfig(nocalhostConfigPath);
            if (configs.size() == 0) {
                Messages.showErrorDialog("Not found config.yaml", "");
            } else if (configs.size() == 1) {
                configPath = configs.get(0);
            } else if (configs.size() > 1) {
                configPath = FileChooseUtil.chooseSingleFile(project, "Please select your configuration file", nocalhostConfigPath, CONFIG_FILE_EXTENSIONS);
            }
            if (configPath == null) {
                return;
            }

            opts.setLocalPath(localPath.toString());
            opts.setConfig(configPath.toString());

        } else {
            AppInstallOrUpgradeOption upgradeOption = askAndGetUpgradeOption(installType);
            if (upgradeOption == null) {
                return;
            }

            if (StringUtils.equals(installType, "helmRepo")) {
                opts.setHelmRepoUrl(context.getApplicationUrl());
                opts.setHelmChartName(applicationName);
                if (upgradeOption.isSpecifyOneSelected()) {
                    opts.setHelmRepoVersion(upgradeOption.getSpecifyText());
                }
            } else {
                opts.setGitUrl(context.getApplicationUrl());
                opts.setConfig(context.getApplicationConfigPath());
                if (upgradeOption.isSpecifyOneSelected()) {
                    opts.setGitRef(upgradeOption.getSpecifyText());
                }
            }

            if (StringUtils.equalsIgnoreCase(installType, "kustomizeGit")) {
                KustomizePathDialog kustomizePathDialog = new KustomizePathDialog(project);
                if (kustomizePathDialog.showAndGet()) {
                    String specifyPath = kustomizePathDialog.getSpecifyPath();
                    if (StringUtils.isNotBlank(specifyPath)) {
                        resourceDirs.add(specifyPath);
                    }
                } else {
                    return;
                }
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
        opts.setResourcesPath(resourceDirs);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Upgrading application: " + applicationName, false) {
            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

                NocalhostNotifier.getInstance(project).notifySuccess("Application " + applicationName + " upgraded", "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    return;
                }
                LOG.error("error occurred while upgrading application", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost upgrade application error", "Error occurred while upgrading application", e.getMessage());
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
                outputCapturedNhctlCommand.upgrade(applicationName, opts);
            }
        });
    }

    private AppInstallOrUpgradeOption askAndGetUpgradeOption(String installType) {
        final String title = "Upgrade DevSpace: " + applicationName;
        AppInstallOrUpgradeOptionDialog dialog;
        if (StringUtils.equals(installType, "helmRepo")) {
            dialog = new AppInstallOrUpgradeOptionDialog(
                    project,
                    title,
                    "Which version to upgrade?",
                    "Latest Version",
                    "Input the version of chart",
                    "Chart version cannot be empty");
        } else if (StringUtils.equals(installType, "kustomizeGit")) {
            dialog = new AppInstallOrUpgradeOptionDialog(
                    project,
                    title,
                    "Which branch to upgrade(Kustomize in Git Repo)?",
                    "Default Branch",
                    "Input the branch of repository",
                    "Git ref cannot be empty");
        } else {
            dialog = new AppInstallOrUpgradeOptionDialog(
                    project,
                    title,
                    "Which branch to upgrade(Manifests in Git Repo)?",
                    "Default Branch",
                    "Input the branch of repository",
                    "Git ref cannot be empty");
        }

        if (!dialog.showAndGet()) {
            return null;
        }

        return dialog.getAppInstallOrUpgradeOption();
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
