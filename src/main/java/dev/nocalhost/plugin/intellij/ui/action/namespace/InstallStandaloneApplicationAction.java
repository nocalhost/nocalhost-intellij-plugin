package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.google.common.collect.Lists;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.data.nocalhostconfig.Application;
import dev.nocalhost.plugin.intellij.data.nocalhostconfig.NocalhostConfig;
import dev.nocalhost.plugin.intellij.task.InstallStandaloneApplicationTask;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.dialog.GitCloneStandabloneApplicationDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.InstallStandaloneHelmRepoApplicationDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.KustomizePathDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.utils.ConfigUtil;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.MessageDialogUtil;

import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_LOCAL;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_REPO;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_KUSTOMIZE_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_KUSTOMIZE_LOCAL;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST_LOCAL;

public class InstallStandaloneApplicationAction extends DumbAwareAction {
    private static final int OPTION_OPEN_LOCAL_DIRECTORY = 0;
    private static final int OPTION_CLONE_FROM_GIT = 1;
    private static final int OPTION_HELM_REPO = 2;

    private final Project project;
    private final NamespaceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private final OutputCapturedGitCommand outputCapturedGitCommand;

    private int installTypeSelectedByUser;
    private String gitUrl;
    private String gitRef;
    private Path localPath;
    private Path configPath;

    public InstallStandaloneApplicationAction(Project project, NamespaceNode node) {
        super("Install Application", "", AllIcons.Actions.Install);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespace();

        outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        installTypeSelectedByUser = MessageDialogUtil.show(
                project,
                "Install Application",
                "Please select the application installation source",
                "Open Local Directory",
                "Clone from Git",
                "Helm Repo",
                "Cancel"
        );
        switch (installTypeSelectedByUser) {
            case OPTION_OPEN_LOCAL_DIRECTORY:
                Path localPath = FileChooseUtil.chooseSingleDirectory(
                        project,
                        "Install Application",
                        "Select local directory which contains application configuration");
                if (localPath == null) {
                    return;
                }
                this.localPath = localPath;
                resolveConfig();
                break;

            case OPTION_CLONE_FROM_GIT:
                configCloneFromGit();
                break;

            case OPTION_HELM_REPO:
                configHelmRepo();
                break;

            default:
        }
    }

    private void configCloneFromGit() {
        GitCloneStandabloneApplicationDialog dialog = new GitCloneStandabloneApplicationDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        gitUrl = dialog.getGitUrl();
        gitRef = dialog.getGitRef();
        gitClone();
    }

    private void gitClone() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path tempDir = Files.createTempDirectory("nocalhost-install-standalone-application-");
                tempDir.toFile().deleteOnExit();
                outputCapturedGitCommand.clone(tempDir.getParent(), gitUrl, tempDir.getFileName().toString());
                if (StringUtils.isNotEmpty(gitRef)) {
                    outputCapturedGitCommand.checkout(tempDir, gitRef);
                }
                localPath = tempDir;
                resolveConfig();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Cloning git repository error",
                        "Error occurs while cloing git repository", e);
            }
        });
    }

    private void resolveConfig() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path nocalhostConfigDirectory = localPath.resolve(".nocalhost");
                List<Path> configs = ConfigUtil.resolveConfigFiles(nocalhostConfigDirectory);
                if (configs.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        MessageDialogUtil.showError(project, "Install Standalone Application", "No nocalhost config found.");
                    });
                } else if (configs.size() == 1) {
                    configPath = configs.get(0);
                    checkInstallType();
                } else {
                    selectConfig(
                            nocalhostConfigDirectory,
                            configs.stream().map(e -> e.getFileName().toString()).collect(Collectors.toSet()));
                }
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Scanning config files error",
                        "Error occurs while scanning config files", e);
            }
        });
    }

    private void selectConfig(Path configDirectory, Set<String> files) {
        ApplicationManager.getApplication().invokeLater(() -> {
            configPath = FileChooseUtil.chooseSingleFile(
                    project,
                    "Please select your configuration file",
                    configDirectory,
                    files);
            if (configPath == null) {
                return;
            }

            checkInstallType();
        });
    }

    private void checkInstallType() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                NocalhostConfig nocalhostConfig = DataUtils.fromYaml(Files.readString(configPath),
                        NocalhostConfig.class);
                Application application = nocalhostConfig.getApplication();
                String installType = application.getManifestType();

                switch (installTypeSelectedByUser) {
                    case OPTION_OPEN_LOCAL_DIRECTORY:
                        if (!Set.of(
                                MANIFEST_TYPE_RAW_MANIFEST_LOCAL,
                                MANIFEST_TYPE_HELM_LOCAL,
                                MANIFEST_TYPE_KUSTOMIZE_LOCAL
                        ).contains(installType)) {
                            MessageDialogUtil.showError(project, "Install Standalone Application",
                                    "Manifest type " + installType + " is not supported.");
                            return;
                        }
                        break;

                    case OPTION_CLONE_FROM_GIT:
                        if (!Set.of(
                                MANIFEST_TYPE_RAW_MANIFEST_GIT,
                                MANIFEST_TYPE_HELM_GIT,
                                MANIFEST_TYPE_KUSTOMIZE_GIT
                        ).contains(installType)) {
                            MessageDialogUtil.showError(project, "Install Standalone Application",
                                    "Manifest type " + installType + " is not supported.");
                            return;
                        }
                        break;

                    default:
                        MessageDialogUtil.showError(project, "Install Standalone Application",
                                "Manifest type " + installType + " is not supported.");
                        return;
                }

                moreConfig(application);
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading nocalhost config error",
                        "Error occurs while loading nocalhost config", e);
            }
        });
    }

    private void moreConfig(Application application) {
        String installType = application.getManifestType();

        List<String> resourceDirs = Lists.newArrayList();
        if (application.getResourcePath() != null) {
            resourceDirs.addAll(application.getResourcePath());
        }

        NhctlInstallOptions opts = new NhctlInstallOptions(kubeConfigPath, namespace);

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

        if (Set.of(MANIFEST_TYPE_HELM_GIT, MANIFEST_TYPE_HELM_LOCAL).contains(installType)) {
            HelmValuesChooseDialog helmValuesChooseDialog = new HelmValuesChooseDialog(project);
            if (helmValuesChooseDialog.showAndGet()) {
                HelmValuesChooseState helmValuesChooseState = helmValuesChooseDialog
                        .getHelmValuesChooseState();
                if (helmValuesChooseState.isSpecifyValuesYamlSelected()) {
                    opts.setHelmValues(helmValuesChooseState.getValuesYamlPath());
                }
                if (helmValuesChooseState.isSpecifyValues()
                        && Set.of(MANIFEST_TYPE_HELM_GIT).contains(installType)) {
                    opts.setValues(helmValuesChooseState.getValues());
                }
            } else {
                return;
            }
        }

        switch (installType) {
            case MANIFEST_TYPE_RAW_MANIFEST_LOCAL:
            case MANIFEST_TYPE_HELM_LOCAL:
            case MANIFEST_TYPE_KUSTOMIZE_LOCAL:
                opts.setLocalPath(localPath.toString());
                opts.setOuterConfig(configPath.toString());
                break;

            case MANIFEST_TYPE_RAW_MANIFEST_GIT:
            case MANIFEST_TYPE_HELM_GIT:
            case MANIFEST_TYPE_KUSTOMIZE_GIT:
                opts.setGitUrl(gitUrl);
                if (StringUtils.isNotEmpty(gitRef)) {
                    opts.setGitRef(gitRef);
                }
                opts.setConfig(localPath.relativize(configPath).toString());
                break;

            default:
        }

        opts.setResourcesPath(resourceDirs);
        opts.setType(installType);

        install(application.getName(), opts);
    }

    private void configHelmRepo() {
        InstallStandaloneHelmRepoApplicationDialog dialog = new InstallStandaloneHelmRepoApplicationDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }

        NhctlInstallOptions opts = new NhctlInstallOptions(kubeConfigPath, namespace);

        HelmValuesChooseDialog helmValuesChooseDialog = new HelmValuesChooseDialog(project);
        if (helmValuesChooseDialog.showAndGet()) {
            HelmValuesChooseState helmValuesChooseState = helmValuesChooseDialog
                    .getHelmValuesChooseState();
            if (helmValuesChooseState.isSpecifyValuesYamlSelected()) {
                opts.setHelmValues(helmValuesChooseState.getValuesYamlPath());
            }
            if (helmValuesChooseState.isSpecifyValues()) {
                opts.setValues(helmValuesChooseState.getValues());
            }
        }

        opts.setHelmChartName(dialog.getName());
        opts.setHelmRepoUrl(dialog.getChartUrl());
        opts.setHelmRepoVersion(dialog.getVersion());
        opts.setType(MANIFEST_TYPE_HELM_REPO);

        install(dialog.getName(), opts);
    }

    private void install(String applicationName, NhctlInstallOptions opts) {
        ProgressManager.getInstance().run(
                new InstallStandaloneApplicationTask(project, applicationName, opts));
    }

}
