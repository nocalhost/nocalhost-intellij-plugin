package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.topic.NocalhostTreeExpandNotifier;

public class NocalhostTreeExpandActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        project.getMessageBus().syncPublisher(
                NocalhostTreeExpandNotifier.NOCALHOST_TREE_EXPAND_NOTIFIER_TOPIC).action();
    }
}
