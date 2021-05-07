package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;

public class AssociateLocalDirectoryAction extends AnAction {
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(
            NocalhostSettings.class);

    private final Project project;
    private final ResourceNode node;

    public AssociateLocalDirectoryAction(Project project, ResourceNode resourceNode) {
        super("Associate Local Directory");

        this.project = project;
        this.node = resourceNode;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Path parentDir = FileChooseUtil.chooseSingleDirectory(project);

        if (parentDir == null || StringUtils.isNotEmpty(parentDir.toString())) {
            return;
        }

        ServiceProjectPath serviceProjectPath = ServiceProjectPath.builder()
                .server(node.getClusterNode().getNocalhostAccount().getServer())
                .username(node.getClusterNode().getNocalhostAccount().getUsername())
                .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                .namespace(node.getNamespaceNode().getName())
                .applicationName(node.applicationName())
                .serviceName(node.resourceName())
                .projectPath(parentDir.toString())
                .build();
        nocalhostSettings.saveServiceProjectPath(serviceProjectPath);
    }
}
