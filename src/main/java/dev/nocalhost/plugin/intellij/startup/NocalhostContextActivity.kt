package dev.nocalhost.plugin.intellij.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager

class NocalhostContextActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        NocalhostContextManager.getInstance(project).refresh()
    }
}
