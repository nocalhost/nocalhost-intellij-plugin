package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import dev.nocalhost.plugin.intellij.nhctl.NhctlDeleteKubeConfigCommand;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class LogoutNocalhostAccountsTask extends Task.Backgroundable {
    private final NocalhostSettings settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);

    private final List<NocalhostAccount> nocalhostAccounts;

    public LogoutNocalhostAccountsTask(Project project, List<NocalhostAccount> nocalhostAccounts) {
        super(project, "Logging out Nocalhost accounts");
        this.nocalhostAccounts = nocalhostAccounts;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        var map = settings.getKubeConfigMap();
        nocalhostAccounts.forEach(it -> {
            var prefix = it.getServer() + ":" + it.getUsername() + ":";
            settings.removeNocalhostAccount(it);
            map.entrySet().removeIf(x -> {
                var matched = x.getKey().startsWith(prefix);
                if (matched) {
                    notifyToNhctl(x.getValue());
                }
                return matched;
            });
        });
        settings.setKubeConfigMap(map);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
    }

    private void notifyToNhctl(@NotNull String kubeConfig) {
        try {
            var cmd = new NhctlDeleteKubeConfigCommand();
            cmd.setKubeConfig(KubeConfigUtil.kubeConfigPath(kubeConfig));
            cmd.execute();
        } catch (Exception ex) {
            ErrorUtil.dealWith(getProject(), "Notify nhctl error", "Error occurs while notify nhctl.", ex);
        }
    }
}
