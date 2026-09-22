package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.google.common.collect.Lists;

import com.intellij.icons.AllIcons;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.data.nocalhostconfig.Application;
import dev.nocalhost.plugin.intellij.data.nocalhostconfig.NocalhostConfig;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.task.InstallDemoTask;
import dev.nocalhost.plugin.intellij.task.InstallStandaloneApplicationTask;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.dialog.ConfigStandaloneHelmRepoApplicationDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.GitCloneStandabloneApplicationDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.KustomizePathDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.ListChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
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

public class InstallStandaloneApplicationAction extends DumbAwareAction {
    private static final int OPTION_OPEN_LOCAL_DIRECTORY = 0;
    private static final int OPTION_CLONE_FROM_GIT = 1;
    private static final int OPTION_HELM_REPO = 2;
    private static final int OPTION_INSTALL_DEMO = 3;

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;

    private final OutputCapturedGitCommand outputCapturedGitCommand;

    private final AtomicInteger installTypeSelectedByUser = new AtomicInteger();
    private final AtomicReference<String> gitUrl = new AtomicReference<>();
    private final AtomicReference<String> gitRef = new AtomicReference<>();
    private final AtomicReference<Path> localPath = new AtomicReference<>();
    private final AtomicReference<Path> configPath = new AtomicReference<>();

    public InstallStandaloneApplicationAction(Project project, NamespaceNode node) {
        super(NocalhostI18n.get("action.deployApplication"), "", AllIcons.Actions.Install);
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespace();

        outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        int installTypeSelectedByUser = Messages.showDialog(
                project,
                NocalhostI18n.get("common.selectDeploySource"),
                NocalhostI18n.get("action.deployApplication"),
                new String[]{
                        NocalhostI18n.get("common.openLocalDirectory"),
                        NocalhostI18n.get("common.cloneFromGit"),
                        NocalhostI18n.get("common.helmRepo"),
                        NocalhostI18n.get("common.deployDemo"),
                        NocalhostI18n.get("common.cancel")},
                0,
                Messages.getQuestionIcon());
        this.installTypeSelectedByUser.set(installTypeSelectedByUser);
        switch (installTypeSelectedByUser) {
            case OPTION_OPEN_LOCAL_DIRECTORY:
                Path localPath = FileChooseUtil.chooseSingleDirectory(
                        project,
                        NocalhostI18n.get("action.deployApplication"),
                        NocalhostI18n.get("common.selectLocalDir"));
                if (localPath == null) {
                    return;
                }
                this.localPath.set(localPath);
                resolveConfig();
                break;

            case OPTION_CLONE_FROM_GIT:
                configCloneFromGit();
                break;

            case OPTION_HELM_REPO:
                configHelmRepo();
                break;

            case OPTION_INSTALL_DEMO:
                ProgressManager.getInstance().run(new InstallDemoTask(project, kubeConfigPath,
                        namespace));
                break;

            default:
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
                Path tempDir = Files.createTempDirectory("nocalhost-install-standalone-application-");
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
                            project, NocalhostI18n.get("common.nocalhostDirNotFound"), NocalhostI18n.get("action.installStandaloneApp")));
                }
                List<Path> configs = ConfigUtil.resolveConfigFiles(nocalhostConfigDirectory, kubeConfigPath, namespace);
                if (configs.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
                            project, NocalhostI18n.get("common.noNocalhostConfig"), NocalhostI18n.get("action.installStandaloneApp")));
                } else if (configs.size() == 1) {
                    configPath.set(configs.get(0));
                    checkInstallType();
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

    private void selectConfig(Path configDirectory, Set<String> files) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ListChooseDialog dialog = new ListChooseDialog(project, NocalhostI18n.get("common.selectConfigFile"),
                    Lists.newArrayList(files));
            dialog.showAndGet();
            String configFile = dialog.getSelectedValue();
            if (!StringUtils.isNotEmpty(configFile)) {
                return;
            }
            this.configPath.set(configDirectory.resolve(configFile));

            checkInstallType();
        });
    }

    private void checkInstallType() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                NocalhostConfig nocalhostConfig = DataUtils.fromYaml(Files.readString(configPath.get()),
                        NocalhostConfig.class);
                Application application = nocalhostConfig.getApplication();
                String installType = application.getManifestType();

                switch (installTypeSelectedByUser.get()) {
                    case OPTION_OPEN_LOCAL_DIRECTORY:
                        if (!Set.of(
                                MANIFEST_TYPE_RAW_MANIFEST_LOCAL,
                                MANIFEST_TYPE_HELM_LOCAL,
                                MANIFEST_TYPE_KUSTOMIZE_LOCAL
                        ).contains(installType)) {
                            Messages.showErrorDialog(
                                    project,
                                    NocalhostI18n.format("common.manifestTypeUnsupported", installType),
                                    NocalhostI18n.get("dialog.deployStandaloneApp"));
                            return;
                        }
                        break;

                    case OPTION_CLONE_FROM_GIT:
                        if (!Set.of(
                                MANIFEST_TYPE_RAW_MANIFEST_GIT,
                                MANIFEST_TYPE_HELM_GIT,
                                MANIFEST_TYPE_KUSTOMIZE_GIT
                        ).contains(installType)) {
                            Messages.showErrorDialog(
                                    project,
                                    NocalhostI18n.format("common.manifestTypeUnsupported", installType),
                                    NocalhostI18n.get("dialog.deployStandaloneApp"));
                            return;
                        }
                        break;

                    default:
                        Messages.showErrorDialog(
                                project,
                                NocalhostI18n.format("common.manifestTypeUnsupported", installType),
                                NocalhostI18n.get("dialog.deployStandaloneApp"));
                        return;
                }

                moreConfig(application);
            } catch (Exception e) {
                ErrorUtil.dealWith(project, NocalhostI18n.get("error.loadNocalhostConfig"),
                        NocalhostI18n.get("error.loadNocalhostConfig.content"), e);
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
                opts.setConfig(configPath.get().getFileName().toString());
                break;

            default:
        }

        opts.setResourcesPath(resourceDirs);
        opts.setType(installType);

        install(application.getName(), opts);
    }

    private void configHelmRepo() {
        ConfigStandaloneHelmRepoApplicationDialog dialog =
                new ConfigStandaloneHelmRepoApplicationDialog(project);
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
