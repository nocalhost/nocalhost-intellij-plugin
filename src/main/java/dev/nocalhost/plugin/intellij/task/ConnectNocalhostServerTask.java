package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.TokenResponse;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import lombok.SneakyThrows;

public class ConnectNocalhostServerTask extends Task.Backgroundable {
    private final NocalhostApi nocalhostApi = ApplicationManager.getApplication().getService(NocalhostApi.class);
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(
            NocalhostSettings.class);

    private final String server;
    private final String username;
    private final String password;

    public ConnectNocalhostServerTask(Project project,
                                      String server,
                                      String username,
                                      String password) {
        super(project, "Connecting to Nocalhost server");
        this.server = server;
        this.username = username;
        this.password = password;
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        TokenResponse resp = nocalhostApi.login(server, username, password);
        UserInfo userInfo = nocalhostApi.getUserInfo(server, resp.getToken());
        nocalhostSettings.updateNocalhostAccount(new NocalhostAccount(
                server, username, resp.getToken(), resp.getRefreshToken(), userInfo));
    }

    @Override
    public void onSuccess() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(getProject()).notifySuccess(
                "Connecting to Nocalhost server success",
                "");
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Connecting to Nocalhost server error",
                "Error occurs while connecting to Nocalhost server", e);
    }
}
