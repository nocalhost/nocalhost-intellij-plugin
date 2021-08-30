package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;

public class LogoutNocalhostAccountsTask extends Task.Backgroundable {
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(NocalhostSettings.class);

    private final List<NocalhostAccount> nocalhostAccounts;

    public LogoutNocalhostAccountsTask(Project project, List<NocalhostAccount> nocalhostAccounts) {
        super(project, "Logging out Nocalhost accounts");
        this.nocalhostAccounts = nocalhostAccounts;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        nocalhostAccounts.forEach(e -> nocalhostSettings.removeNocalhostAccount(e));
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
    }
}
