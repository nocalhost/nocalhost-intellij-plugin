package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;

import dev.nocalhost.plugin.intellij.task.ExecutionTask;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class RunAction extends StartDevelopAction {
    public RunAction(Project project, ResourceNode node) {
        super("Run", project, node, AllIcons.Actions.Execute, ExecutionTask.kRun);
    }
}
