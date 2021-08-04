package dev.nocalhost.plugin.intellij.ui.action.workload

import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfiguration
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfigurationFactory
import dev.nocalhost.plugin.intellij.configuration.python.NocalhostPythonConfigurationType
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode

class DebugAction: DumbAwareAction {
    var project: Project
    var node: ResourceNode

    constructor(project: Project, node: ResourceNode): super("Debug") {
        this.node = node
        this.project = project
    }

    private fun getConfiguration(): RunnerAndConfigurationSettings {
        var manager = RunManager.getInstance(project);
        var conf: RunnerAndConfigurationSettings? = null
        var list = manager.getConfigurationSettingsList(NocalhostPythonConfigurationType::class.java);
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
        var builder = ExecutionEnvironmentBuilder.createOrNull(DefaultDebugExecutor.getDebugExecutorInstance(), getConfiguration())
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build())
        }
    }
}