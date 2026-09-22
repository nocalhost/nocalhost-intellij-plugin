package dev.nocalhost.plugin.intellij.ui.action.application;

import com.google.common.collect.Lists;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUpgradeOptions;
import dev.nocalhost.plugin.intellij.data.nocalhostconfig.Application;
import dev.nocalhost.plugin.intellij.data.nocalhostconfig.NocalhostConfig;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.task.UpgradeStandaloneApplicationTask;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.dialog.ConfigStandaloneHelmRepoApplicationDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.GitCloneStandabloneApplicationDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.KustomizePathDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.ConfigUtil;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_LOCAL;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_HELM_REPO;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_KUSTOMIZE_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_KUSTOMIZE_LOCAL;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST_GIT;
import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST_LOCAL;

public class UpgradeStandaloneApplicationAction extends DumbAwareAction {
    private final Project project;
    private final ApplicationNode node;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;
    private final String applicationType;

    private final OutputCapturedGitCommand outputCapturedGitCommand;

    private final AtomicReference<String> gitUrl = new AtomicReference<>();
    private final AtomicReference<String> gitRef = new AtomicReference<>();
    private final AtomicReference<Path> localPath = new AtomicReference<>();
    private final AtomicReference<Path> configPath = new AtomicReference<>();

    public UpgradeStandaloneApplicationAction(Project project, ApplicationNode node) {
        super(NocalhostI18n.get("action.upgradeStandaloneApp"));
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        this.applicationName = node.getName();
        this.applicationType = node.getApplication().getType();

        outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        switch (applicationType) {
            case MANIFEST_TYPE_RAW_MANIFEST_LOCAL:
            case MANIFEST_TYPE_HELM_LOCAL:
            case MANIFEST_TYPE_KUSTOMIZE_LOCAL:
                Path localPath = FileChooseUtil.chooseSingleDirectory(
                        project,
                        NocalhostI18n.get("dialog.upgradeStandaloneApp"),
                        NocalhostI18n.get("common.selectLocalDir"));
                if (localPath == null) {
                    return;
                }
                this.localPath.set(localPath);
                resolveConfig();
                break;

            case MANIFEST_TYPE_RAW_MANIFEST_GIT:
            case MANIFEST_TYPE_HELM_GIT:
            case MANIFEST_TYPE_KUSTOMIZE_GIT:
                configCloneFromGit();
                break;

            case MANIFEST_TYPE_HELM_REPO:
                configHelmRepo();
                break;

            default:
                Messages.showErrorDialog(NocalhostI18n.format("error.unableUpgrade", node.getApplication().getType()), NocalhostI18n.get("action.upgradeStandaloneApp"));
                break;
        }
    }

    private void configCloneFromGit() {
        GitCloneStandabloneApplicationDialog dialog = new GitCloneStandabloneApplicationDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        gitUrl.set(dialog.getGitUrl());
        gitRef.set(dialog.getGitRef());
        gitClone();
    }

    private void gitClone() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path tempDir = Files.createTempDirectory("nocalhost-upgrade-standalone-application-");
                tempDir.toFile().deleteOnExit();
                outputCapturedGitCommand.clone(tempDir, gitUrl.get(), gitRef.get());
                localPath.set(tempDir);
                resolveConfig();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, NocalhostI18n.get("error.cloneGitRepo"),
                        NocalhostI18n.get("error.cloneGitRepo.content"), e);
            }
        });
    }

    private void resolveConfig() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path nocalhostConfigDirectory = localPath.get().resolve(".nocalhost");
                if (!Files.exists(nocalhostConfigDirectory)) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
                            project, NocalhostI18n.get("common.nocalhostDirNotFound"), NocalhostI18n.get("dialog.upgradeStandaloneApp")));
                }
                List<Path> configs = ConfigUtil.resolveConfigFiles(nocalhostConfigDirectory, kubeConfigPath, namespace);
                if (configs.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
                            project, NocalhostI18n.get("common.noNocalhostConfig"), NocalhostI18n.get("dialog.upgradeStandaloneApp")));
                } else if (configs.size() == 1) {
                    configPath.set(configs.get(0));
                    checkApplicationType();
                } else {
                    selectConfig(
                            nocalhostConfigDirectory,
                            configs.stream().map(e -> e.getFileName().toString()).collect(Collectors.toSet()));
                }
            } catch (Exception e) {
                ErrorUtil.dealWith(project, NocalhostI18n.get("error.scanConfigFiles"),
                        NocalhostI18n.get("error.scanConfigFiles.content"), e);
            }
        });
    }

    private void checkApplicationType() {
        try {
            NocalhostConfig nocalhostConfig = DataUtils.fromYaml(Files.readString(configPath.get()),
                    NocalhostConfig.class);
            Application application = nocalhostConfig.getApplication();
            String upgradeType = application.getManifestType();
            if (StringUtils.equals(upgradeType, applicationType)) {
                moreConfig(application);
            } else {
                Messages.showErrorDialog(
                        project,
                        NocalhostI18n.format("common.manifestTypeNotMatched", upgradeType, applicationType),
                        NocalhostI18n.get("dialog.upgradeStandaloneApp"));
            }
        } catch (Exception e) {
            ErrorUtil.dealWith(project, NocalhostI18n.get("error.loadNocalhostConfig"),
                    NocalhostI18n.get("error.loadNocalhostConfig.content"), e);
        }
    }

    private void moreConfig(Application application) {
        String upgradeType = application.getManifestType();

        List<String> resourceDirs = Lists.newArrayList();
        if (application.getResourcePath() != null) {
            resourceDirs.addAll(application.getResourcePath());
        }

        NhctlUpgradeOptions opts = new NhctlUpgradeOptions(kubeConfigPath, namespace);

        if (Set.of(MANIFEST_TYPE_KUSTOMIZE_GIT, MANIFEST_TYPE_KUSTOMIZE_LOCAL).contains(upgradeType)) {
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

        if (Set.of(MANIFEST_TYPE_HELM_GIT, MANIFEST_TYPE_HELM_LOCAL).contains(upgradeType)) {
            HelmValuesChooseDialog helmValuesChooseDialog = new HelmValuesChooseDialog(project);
            if (helmValuesChooseDialog.showAndGet()) {
                HelmValuesChooseState helmValuesChooseState = helmValuesChooseDialog
                        .getHelmValuesChooseState();
                if (helmValuesChooseState.isSpecifyValuesYamlSelected()) {
                    opts.setHelmValues(helmValuesChooseState.getValuesYamlPath());
                }
                if (helmValuesChooseState.isSpecifyValues()
                        && Set.of(MANIFEST_TYPE_HELM_GIT).contains(upgradeType)) {
                    opts.setValues(helmValuesChooseState.getValues());
                }
            } else {
                return;
            }
        }

        switch (upgradeType) {
            case MANIFEST_TYPE_RAW_MANIFEST_LOCAL:
            case MANIFEST_TYPE_HELM_LOCAL:
            case MANIFEST_TYPE_KUSTOMIZE_LOCAL:
                opts.setLocalPath(localPath.get().toString());
                opts.setOuterConfig(configPath.get().toString());
                break;

            case MANIFEST_TYPE_RAW_MANIFEST_GIT:
            case MANIFEST_TYPE_HELM_GIT:
            case MANIFEST_TYPE_KUSTOMIZE_GIT:
                opts.setGitUrl(gitUrl.get());
                if (StringUtils.isNotEmpty(gitRef.get())) {
                    opts.setGitRef(gitRef.get());
                }
                opts.setConfig(localPath.get().relativize(configPath.get()).toString());
                break;

            default:
        }

        opts.setResourcesPath(resourceDirs);

        upgrade(opts);
    }

    private void selectConfig(Path configDirectory, Set<String> files) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Path configPath = FileChooseUtil.chooseSingleFile(
                    project,
                    NocalhostI18n.get("common.selectConfigFile"),
                    configDirectory,
                    files);
            if (configPath == null) {
                return;
            }
            this.configPath.set(configPath);

            checkApplicationType();
        });
    }

    private void configHelmRepo() {
        ConfigStandaloneHelmRepoApplicationDialog dialog =
                new ConfigStandaloneHelmRepoApplicationDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }

        NhctlUpgradeOptions opts = new NhctlUpgradeOptions(kubeConfigPath, namespace);

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

        upgrade(opts);
    }

    private void upgrade(NhctlUpgradeOptions opts) {
        ProgressManager.getInstance().run(
                new UpgradeStandaloneApplicationTask(project, applicationName, opts));
    }
}
