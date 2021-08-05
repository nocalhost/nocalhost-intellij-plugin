package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.task.ExecuteTask;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class RunAction extends StartDevelopAction {
    public RunAction(Project project, ResourceNode node) {
        super("Run", project, node, null);
        this.onAfter = () -> ProgressManager
                .getInstance()
                .run(new ExecuteTask(project, project.getService(NocalhostProjectSettings.class).getDevModeService(), false));
    }
}
