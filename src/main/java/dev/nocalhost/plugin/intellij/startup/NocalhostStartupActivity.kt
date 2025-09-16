package dev.nocalhost.plugin.intellij.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask
import org.apache.commons.lang3.StringUtils
import java.nio.file.Paths

class NocalhostStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        devStart(project)
    }

    private fun devStart(project: Project) {
        val path = project.basePath
        if (StringUtils.isEmpty(path)) {
            return
        }
        val settings = ApplicationManager.getApplication()
            .getService(NocalhostSettings::class.java)
        val projectPath = Paths.get(path).toString()
        val devModeService = settings.getDevModeServiceByProjectPath(projectPath)
        if (devModeService != null) {
            try {
                ProgressManager.getInstance().run(StartingDevModeTask(project, devModeService))
            } catch (e: Exception) {
                LOG.error("error occurred while starting develop", e)
                NocalhostNotifier.getInstance(project).notifyError(
                    "Nocalhost starting dev mode error",
                    "Error occurred while starting dev mode",
                    e.message!!
                )
            } finally {
                settings.removeDevModeServiceByProjectPath(projectPath)
            }
        }
    }


    companion object {
        private val LOG = Logger.getInstance(NocalhostStartupActivity::class.java)
    }
}
