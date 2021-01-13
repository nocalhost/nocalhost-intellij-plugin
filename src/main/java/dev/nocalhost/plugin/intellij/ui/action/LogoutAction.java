package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;

public class LogoutAction extends AnAction implements DumbAware {

    public LogoutAction() {
        super("Sign out", "Sign out nocalhost server", AllIcons.Actions.Exit);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        nocalhostSettings.clearAuth();

        final Application application = ApplicationManager.getApplication();
        NocalhostAccountChangedNotifier publisher = application.getMessageBus()
                .syncPublisher(NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC);
        publisher.action();
    }
}
