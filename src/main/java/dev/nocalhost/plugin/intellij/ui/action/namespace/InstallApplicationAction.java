package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.google.common.collect.Lists;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.collections.CollectionUtils;
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
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplicationOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.task.InstallApplicationTask;
import dev.nocalhost.plugin.intellij.ui.AppInstallOrUpgradeOption;
import dev.nocalhost.plugin.intellij.ui.AppInstallOrUpgradeOptionDialog;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.InstallApplicationChooseDialog;
import dev.nocalhost.plugin.intellij.ui.KustomizePathDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.HelmNocalhostConfigUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class InstallApplicationAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(InstallApplicationAction.class);
    private static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

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
        this.namespace = node.getName();
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
                List<NhctlListApplication> nhctlListApplications = nhctlCommand.listApplication(
                        new NhctlListApplicationOptions(kubeConfigPath, namespace));

                ApplicationManager.getApplication().invokeLater(() -> {
                    install(applications, nhctlListApplications);
                });

            } catch (IOException
                    | NocalhostApiException
                    | InterruptedException
                    | NocalhostExecuteCmdException e) {
                LOG.error(
                        "error occurred while listing necessary data before installing application",
                        e
                );
            }
        });
    }

    private void install(List<Application> applications,
                         List<NhctlListApplication> nhctlListApplications) {
        final Set<String> apps = applications.stream()
                .map(a -> a.getContext().getApplicationName())
                .collect(Collectors.toSet());
        final Optional<NhctlListApplication> currentDevspacesApp = nhctlListApplications.stream()
                .filter(d -> d.getNamespace().equals(namespace)).findFirst();
        List<String> installed = null;
        if (currentDevspacesApp.isPresent()) {
            installed = currentDevspacesApp.get().getApplication().stream()
                    .map(NhctlListApplication.Application::getName)
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(installed)) {
            for (String installedApp : installed) {
                apps.remove(installedApp);
            }
        }
        if (apps.size() == 0) {
            Messages.showMessageDialog("All applications are installed.", "Install Application", null);
            return;
        }

        InstallApplicationChooseDialog dialog = new InstallApplicationChooseDialog(
                Lists.newArrayList(apps));
        if (dialog.showAndGet()) {
            final Optional<Application> app = applications.stream()
                    .filter(a ->
                            StringUtils.equals(
                                    dialog.getSelected(),
                                    a.getContext().getApplicationName()
                            )
                    )
                    .findFirst();
            app.ifPresent(application -> ApplicationManager.getApplication()
                    .executeOnPooledThread(() -> {
                        try {
                            if (NhctlHelper.isApplicationInstalled(kubeConfigPath, namespace,
                                    application.getContext().getApplicationName())) {
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Messages.showMessageDialog("Application has been installed.",
                                            "Install Application", null);
                                });
                                return;
                            }
                            ApplicationManager.getApplication().invokeLater(() -> {
                                try {
                                    installApp(application);
                                } catch (IOException e) {
                                    LOG.error("error occurred while installing application", e);
                                }
                            });
                        } catch (IOException | InterruptedException e) {
                            LOG.error("error occurred while checking if application was installed",
                                    e);
                        }
                    }));

        }
    }

    private void installApp(Application app) throws IOException {
        final Application.Context context = app.getContext();
        final String installType = NhctlHelper.generateInstallType(context);

        final NhctlInstallOptions opts = new NhctlInstallOptions(kubeConfigPath, namespace);
        opts.setType(installType);

        List<String> resourceDirs = Lists.newArrayList(context.getResourceDir());

        if (Set.of("helmLocal", "rawManifestLocal").contains(installType)) {
            String message = StringUtils.equals(installType, "rawManifestLocal")
                    ? "Please choose application manifest root directory"
                    : "Please choose unpacked application helm chart root directory";

            Path localPath = FileChooseUtil.chooseSingleDirectory(project, message);
            if (localPath == null) {
                return;
            }

            Path configPath = null;
            Path nocalhostConfigPath = localPath.resolve(".nocalhost");
            List<Path> configs = getAllConfig(nocalhostConfigPath);
            if (configs.size() == 0) {
                configPath = localPath;
            } else if (configs.size() == 1) {
                configPath = configs.get(0);
            } else if (configs.size() > 1) {
                configPath = FileChooseUtil.chooseSingleFile(
                        project,
                        "Please select your configuration file",
                        nocalhostConfigPath,
                        CONFIG_FILE_EXTENSIONS);
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

            if (StringUtils.equals(installType, "helmRepo")) {
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

        if (Set.of("helmGit", "helmRepo", "helmLocal").contains(installType)) {
            HelmValuesChooseDialog helmValuesChooseDialog = new HelmValuesChooseDialog(project);
            if (helmValuesChooseDialog.showAndGet()) {
                HelmValuesChooseState helmValuesChooseState = helmValuesChooseDialog
                        .getHelmValuesChooseState();
                if (helmValuesChooseState.isSpecifyValuesYamlSelected()) {
                    opts.setHelmValues(helmValuesChooseState.getValuesYamlPath());
                }
                if (helmValuesChooseState.isSpecifyValues()
                        && Set.of("helmGit", "helmRepo").contains(installType)) {
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
        if (StringUtils.equals(installType, "helmRepo")) {
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

    private List<Path> getAllConfig(Path localPath) throws IOException {
        if (Files.notExists(localPath)) {
            return Lists.newArrayList();
        }

        return Files.list(localPath)
                .filter(Files::isRegularFile)
                .filter(e ->
                        CONFIG_FILE_EXTENSIONS.contains(
                                com.google.common.io.Files.getFileExtension(
                                        e.getFileName().toString())
                        )
                )
                .map(Path::toAbsolutePath)
                .collect(Collectors.toList());
    }
}
