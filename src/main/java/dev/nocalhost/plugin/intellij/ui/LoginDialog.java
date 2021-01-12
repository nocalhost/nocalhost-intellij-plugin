package dev.nocalhost.plugin.intellij.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.data.AuthData;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
import dev.nocalhost.plugin.intellij.utils.CommonUtils;


public class LoginDialog extends DialogWrapper {

    private final Logger log;

    private final LoginPanel loginPanel;
    private final Project project;
    private final NocalhostSettings nocalhostSettings;
    private final CommonUtils commonUtils;

    protected LoginDialog(@Nullable Project project, NocalhostSettings nocalhostSettings, CommonUtils commonUtils, Logger log) {
        super(project, true);
        this.nocalhostSettings = nocalhostSettings;
        this.commonUtils = commonUtils;
        this.log = log;
        this.project = project;
        loginPanel = new LoginPanel(this);
//        loginPanel.setHost(nocalhostSettings.getHost());
//        loginPanel.setEmail(nocalhostSettings.getEmail());
//        loginPanel.setPassword(nocalhostSettings.getPassword());

        loginPanel.setHost("http://106.55.223.21:8080/");
        loginPanel.setEmail("fatjyc@gmail.com");
        loginPanel.setPassword("123123");
        setTitle("Login to Nocalhost");
        setOKButtonText("Login");
        init();
    }

    @Override
    @NotNull
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    protected void doHelpAction() {
        BrowserUtil.browse("https://nocalhost.dev");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return loginPanel.getPanel();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return loginPanel.getPreferrableFocusComponent();
    }

    @Override
    protected void doOKAction() {
        final String email = loginPanel.getEmail();
        final String password = loginPanel.getPassword();
        final String host = loginPanel.getHost();
        AuthData authData = new AuthData(host, email);
        try {
            String token = commonUtils.checkCredentials(project, authData, password);
            if (StringUtils.isNotBlank(token)) {
                nocalhostSettings.setEmail(email);
                nocalhostSettings.setPassword(password);
                nocalhostSettings.setHost(host);
                nocalhostSettings.setToken(token);
                UserInfo userInfo = commonUtils.decodedJWT(token);

                authData.setToken(token);
                authData.setUser(userInfo);
                nocalhostSettings.setAuth(authData);

                final Application application = ApplicationManager.getApplication();
                NocalhostAccountChangedNotifier publisher = application.getMessageBus()
                                                                       .syncPublisher(NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC);
                publisher.action();

                super.doOKAction();
            } else {
                setErrorText("Can't login with given credentials");
            }
        } catch (Exception e) {
            log.info(e);
            setErrorText("Can't login: " + commonUtils.getErrorTextFromException(e));
        }
    }

    public void clearErrors() {
        setErrorText(null);
    }
}
