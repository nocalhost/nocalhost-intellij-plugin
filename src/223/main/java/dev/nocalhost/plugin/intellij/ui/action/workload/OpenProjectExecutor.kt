package dev.nocalhost.plugin.intellij.ui.action.workload

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.impl.OpenProjectTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Paths

class OpenProjectExecutor {
    companion object {
        @JvmStatic
        fun open(projectPath: String) {
            val task = OpenProjectTask.build()
            CoroutineScope(Dispatchers.Default).launch {
                RecentProjectsManagerBase.getInstanceEx()
                    .openProject(Paths.get(projectPath), task.withRunConfigurators())
            }
        }
    }
}
