package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.google.common.collect.Lists;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.exception.NocalhostServerVersionOutDatedException;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.task.InstallApplicationTask;
import dev.nocalhost.plugin.intellij.ui.AppInstallOrUpgradeOption;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.dialog.AppInstallOrUpgradeOptionDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.InstallApplicationChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.KustomizePathDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.utils.ConfigUtil;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.HelmNocalhostConfigUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_LOCAL;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_REPO;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_KUSTOMIZE_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_KUSTOMIZE_LOCAL;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST_LOCAL;

public class InstallApplicationAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(InstallApplicationAction.class);

    private final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

    private final Project project;
    private final NamespaceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public InstallApplicationAction(Project project, NamespaceNode node) {
        super("Install Application", "", AllIcons.Actions.Install);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NocalhostAccount nocalhostAccount = node.getClusterNode().getNocalhostAccount();
                List<Application> applications = nocalhostApi.listApplications(
                        nocalhostAccount.getServer(),
                        nocalhostAccount.getJwt(),
                        nocalhostAccount.getUserInfo().getId()
                );

                List<NhctlListApplication> installedApplications = nhctlCommand.getApplications(
                        new NhctlGetOptions(kubeConfigPath, namespace));

                List<Application> availableApplications = applications.stream()
                        .filter(application -> installedApplications.get(0)
                                .getApplication()
                                .stream()
                                .noneMatch(e -> StringUtils.equals(e.getName(), application.getContext().getApplicationName()))
                        ).collect(Collectors.toList());

                selectApplication(availableApplications);

            } catch (NocalhostServerVersionOutDatedException e) {
                NocalhostNotifier.getInstance(project).notifyError("Server version out-dated", e.getMessage());
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Check application status error",
                        "Error occurs while checking application status", e);
            }
        });
    }

    private void selectApplication(List<Application> applications) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (applications.isEmpty()) {
                Messages.showMessageDialog("All applications are installed.", "Install Application", null);
                return;
            }

            List<String> applicationsToBeInstalled = applications.stream()
                    .map(e -> e.getContext().getApplicationName())
                    .sorted()
                    .collect(Collectors.toList());

            InstallApplicationChooseDialog dialog = new InstallApplicationChooseDialog(
                    applicationsToBeInstalled);
            if (dialog.showAndGet()) {
                String applicationName = dialog.getSelected();
                Optional<Application> applicationOptional = applications.stream()
                        .filter(e -> StringUtils.equals(e.getContext().getApplicationName(), applicationName))
                        .findFirst();
                if (applicationOptional.isEmpty()) {
                    return;
                }
                try {
                    installApp(applicationOptional.get());
                } catch (Exception e) {
                    ErrorUtil.dealWith(project, "Install application error",
                            "Error occurs while installing application", e);
                }
            }
        });
    }

    private void installApp(Application app) throws IOException {
        final Application.Context context = app.getContext();
        final String installType = app.getApplicationType();

        final NhctlInstallOptions opts = new NhctlInstallOptions(kubeConfigPath, namespace);
        opts.setType(installType);

        List<String> resourceDirs = Lists.newArrayList(context.getResourceDir());

        if (Set.of(MANIFEST_TYPE_HELM_LOCAL, MANIFEST_TYPE_RAW_MANIFEST_LOCAL, MANIFEST_TYPE_KUSTOMIZE_LOCAL).contains(installType)) {
            Path localPath = FileChooseUtil.chooseSingleDirectory(project, "", "Choose local directory");
            if (localPath == null) {
                return;
            }

            Path configPath;
            Path nocalhostConfigPath = localPath.resolve(".nocalhost");
            List<Path> configs = ConfigUtil.resolveConfigFiles(nocalhostConfigPath);
            if (configs.size() == 0) {
                configPath = localPath;
            } else if (configs.size() == 1) {
                configPath = configs.get(0);
            } else {
                configPath = FileChooseUtil.chooseSingleFile(
                        project,
                        "Please select your configuration file",
                        nocalhostConfigPath,
                        configs.stream().map(e -> e.getFileName().toString()).collect(Collectors.toSet()));
            }
            if (configPath == null) {
                return;
            }

            opts.setLocalPath(localPath.toString());
            opts.setOuterConfig(configPath.toString());

        } else {
            AppInstallOrUpgradeOption installOption = askAndGetInstallOption(installType, app);
            if (installOption == null) {
                return;
            }

            if (StringUtils.equals(installType, MANIFEST_TYPE_HELM_REPO)) {
                opts.setHelmRepoUrl(context.getApplicationUrl());
                opts.setHelmChartName(context.getApplicationName());
                opts.setOuterConfig(HelmNocalhostConfigUtil.helmNocalhostConfigPath(app).toString());
                if (installOption.isSpecifyOneSelected()) {
                    opts.setHelmRepoVersion(installOption.getSpecifyText());
                }
            } else {
                opts.setGitUrl(context.getApplicationUrl());
                opts.setConfig(context.getApplicationConfigPath());
                if (installOption.isSpecifyOneSelected()) {
                    opts.setGitRef(installOption.getSpecifyText());
                }
            }
        }


        if (Set.of(MANIFEST_TYPE_KUSTOMIZE_GIT, MANIFEST_TYPE_KUSTOMIZE_LOCAL).contains(installType)) {
            KustomizePathDialog kustomizePathDialog = new KustomizePathDialog(project);
            if (kustomizePathDialog.showAndGet()) {
                String specifyPath = kustomizePathDialog.getSpecifyPath();
                if (StringUtils.isNotEmpty(specifyPath)) {
                    resourceDirs.add(specifyPath);
                }
            } else {
                return;
            }
        }

        if (Set.of(MANIFEST_TYPE_HELM_GIT, MANIFEST_TYPE_HELM_REPO, MANIFEST_TYPE_HELM_LOCAL).contains(installType)) {
            HelmValuesChooseDialog helmValuesChooseDialog = new HelmValuesChooseDialog(project);
            if (helmValuesChooseDialog.showAndGet()) {
                HelmValuesChooseState helmValuesChooseState = helmValuesChooseDialog
                        .getHelmValuesChooseState();
                if (helmValuesChooseState.isSpecifyValuesYamlSelected()) {
                    opts.setHelmValues(helmValuesChooseState.getValuesYamlPath());
                }
                if (helmValuesChooseState.isSpecifyValues()
                        && Set.of(MANIFEST_TYPE_HELM_GIT, MANIFEST_TYPE_HELM_REPO).contains(installType)) {
                    opts.setValues(helmValuesChooseState.getValues());
                }
            } else {
                return;
            }
        }
        opts.setResourcesPath(resourceDirs);

        ProgressManager.getInstance().run(new InstallApplicationTask(project, app, opts));
    }

    private AppInstallOrUpgradeOption askAndGetInstallOption(String installType, Application app) {
        final String title = "Install DevSpace: " + app.getContext().getApplicationName();
        AppInstallOrUpgradeOptionDialog dialog;
        if (StringUtils.equals(installType, MANIFEST_TYPE_HELM_REPO)) {
            dialog = new AppInstallOrUpgradeOptionDialog(
                    project,
                    title,
                    "Which version to install?",
                    "Default Version",
                    "Input the version of chart",
                    "Chart version cannot be empty");
        } else if (StringUtils.equals(installType, "kustomizeGit")) {
            dialog = new AppInstallOrUpgradeOptionDialog(
                    project,
                    title,
                    "Which branch to install(Kustomize in Git Repo)?",
                    "Default Branch",
                    "Input the branch of repository",
                    "Git ref cannot be empty");
        } else {
            dialog = new AppInstallOrUpgradeOptionDialog(
                    project,
                    title,
                    "Which branch to install(Manifests in Git Repo)?",
                    "Default Branch",
                    "Input the branch of repository",
                    "Git ref cannot be empty");
        }

        if (!dialog.showAndGet()) {
            return null;
        }

        return dialog.getAppInstallOrUpgradeOption();
    }
}
