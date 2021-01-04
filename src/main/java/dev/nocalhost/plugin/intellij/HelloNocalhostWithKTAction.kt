package dev.nocalhost.plugin.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages

class HelloNocalhostWithKTAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(PlatformDataKeys.PROJECT)

        val title = "Nocalhost Plugin"
        val msg = "Hello world with Kotlin!"

        Messages.showMessageDialog(project, msg, title, null)
    }
}