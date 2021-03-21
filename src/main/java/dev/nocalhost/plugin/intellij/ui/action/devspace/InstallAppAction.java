package dev.nocalhost.plugin.intellij.ui.action.devspace;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
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

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.task.InstallAppTask;
import dev.nocalhost.plugin.intellij.ui.AppInstallOrUpgradeOption;
import dev.nocalhost.plugin.intellij.ui.AppInstallOrUpgradeOptionDialog;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseDialog;
import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.HelmNocalhostConfigUtil;

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

        final NhctlInstallOptions opts = new NhctlInstallOptions(devSpace);
        opts.setType(installType);
        opts.setResourcesPath(Arrays.asList(context.getResourceDir()));
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
            Path nocalhostConfigPath = localPath.resolve(".nocalhost");
            List<Path> configs = getAllConfig(nocalhostConfigPath);
            if (configs.size() == 0) {
                configPath = localPath;
            } else if (configs.size() == 1) {
                configPath = configs.get(0);
            } else if (configs.size() > 1) {
                configPath = FileChooseUtil.chooseSingleFile(project, "Please select your configuration file", nocalhostConfigPath, CONFIG_FILE_EXTENSIONS);
            }
            if (configPath == null) {
                return;
            }

            opts.setLocalPath(localPath.toString());
            opts.setOuterConfig(configPath.toString());

        } else {
            AppInstallOrUpgradeOption installOption = askAndGetInstallOption(installType, devSpace);
            if (installOption == null) {
                return;
            }

            if (StringUtils.equals(installType, "helmRepo")) {
                opts.setHelmRepoUrl(context.getApplicationUrl());
                opts.setHelmChartName(context.getApplicationName());
                opts.setOuterConfig(HelmNocalhostConfigUtil.helmNocalhostConfigPath(devSpace).toString());
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

        ProgressManager.getInstance().run(new InstallAppTask(project, devSpace, opts));
    }

    private AppInstallOrUpgradeOption askAndGetInstallOption(String installType, DevSpace devSpace) {
        final String title = "Install DevSpace: " + devSpace.getSpaceName();
        AppInstallOrUpgradeOptionDialog dialog;
        if (StringUtils.equals(installType, "helmRepo")) {
            dialog = new AppInstallOrUpgradeOptionDialog(
                    project,
                    title,
                    "Which version to install?",
                    "Default Version",
                    "Input the version of chart",
                    "Chart version cannot be empty");
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
                .filter(e -> CONFIG_FILE_EXTENSIONS.contains(com.google.common.io.Files.getFileExtension(e.getFileName().toString())))
                .map(Path::toAbsolutePath)
                .collect(Collectors.toList());
    }
}
