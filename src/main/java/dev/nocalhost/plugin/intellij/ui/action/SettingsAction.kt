package dev.nocalhost.plugin.intellij.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages

class SettingsAction :
    AnAction("Settings", "Open Gerrit Plugin Settings", AllIcons.General.Settings), DumbAware {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.getData(PlatformDataKeys.PROJECT)
        val title = "Nocalhost Plugin"
        val msg = "Refresh data!"

        Messages.showMessageDialog(project, msg, title, null)
    }
}
