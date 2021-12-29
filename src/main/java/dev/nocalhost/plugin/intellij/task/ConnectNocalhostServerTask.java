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
import lombok.SneakyThrows;

public class ConnectNocalhostServerTask extends Task.Modal {
    private final NocalhostApi nocalhostApi = ApplicationManager.getApplication().getService(NocalhostApi.class);
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(
            NocalhostSettings.class);

    private final String server;
    private final String username;
    private final String password;
    private final Runnable onSuccess;
    private final Runnable onFailure;

    public ConnectNocalhostServerTask(Project project,
                                      String server,
                                      String username,
                                      String password,
                                      Runnable onSuccess,
                                      Runnable onFailure) {
        super(project, "Connecting to Nocalhost Server", false);
        this.server = server;
        this.username = username;
        this.password = password;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
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
        super.onSuccess();
        ApplicationManager.getApplication().invokeLater(onSuccess);
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(getProject()).notifySuccess(
                "Connected to Nocalhost server successfully",
                "");
    }

    @Override
    public void onThrowable(@NotNull Throwable ex) {
        ApplicationManager.getApplication().invokeLater(() -> {
            onFailure.run();
            NocalhostNotifier
                    .getInstance(getProject())
                    .notifyError("Failed to connect to Nocalhost server", "Error occurred while connecting to Nocalhost server", ex.getMessage());
        });
    }
}
