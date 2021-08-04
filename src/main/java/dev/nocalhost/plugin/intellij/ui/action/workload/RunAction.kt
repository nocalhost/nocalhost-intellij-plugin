package dev.nocalhost.plugin.intellij.ui.action.workload

import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfigurationType
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode

class RunAction: DumbAwareAction {
    var project: Project
    var node: ResourceNode

    constructor(project: Project, node: ResourceNode): super("Run") {
        this.node = node
        this.project = project
    }

    private fun getConf(): RunnerAndConfigurationSettings {
        var manager = RunManager.getInstance(project);
        var list = manager.getConfigurationSettingsList(NocalhostPythonConfigurationType::class.java);
        var conf: RunnerAndConfigurationSettings? = null
        if (list.isEmpty()) {
            conf = manager.createConfiguration("demo", NocalhostPythonConfigurationType::class.java)
            manager.addConfiguration(conf);
        } else {
            conf = list[0]
        }
        manager.selectedConfiguration = conf;
        return conf;
    }

    override fun actionPerformed(e: AnActionEvent) {
        var builder = ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), getConf())
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build())
        }
    }
}