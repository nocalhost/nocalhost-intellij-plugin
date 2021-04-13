package dev.nocalhost.plugin.intellij.ui;

import com.github.zafarkhaja.semver.Version;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
import dev.nocalhost.plugin.intellij.ui.action.LogoutAction;
import dev.nocalhost.plugin.intellij.ui.action.RefreshAction;
import dev.nocalhost.plugin.intellij.ui.action.SettingAction;
import dev.nocalhost.plugin.intellij.ui.tree.NocalhostTree;

public class NocalhostWindow implements Disposable {
    private static final Logger LOG = Logger.getInstance(NocalhostWindow.class);

    private final Project project;

    private final SimpleToolWindowPanel panel;
    private NocalhostTree tree;

    public NocalhostWindow(Project project) {
        this.project = project;

        checkNocalhostVersion();

        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect(this).subscribe(
                NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC,
                NocalhostWindow.this::toggleContent
        );

        panel = new SimpleToolWindowPanel(true, false);

        toggleContent();
    }

    private void checkNocalhostVersion() {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        try {
            String versionInfo = nhctlCommand.version();
            String version = "";
            final String[] infos = StringUtils.split(versionInfo, "\n");
            final Optional<String> versionLine = Arrays.stream(infos).filter(s -> s.trim().startsWith("Version")).findFirst();
            if (versionLine.isPresent()) {
                final String[] versionLines = versionLine.get().split("v");
                if (versionLines.length != 2) {
                    return;
                }
                version = versionLines[1];
            }
            if (StringUtils.isBlank(version)) {
                return;
            }

            InputStream in = NocalhostWindow.class.getClassLoader().getResourceAsStream("config.properties");
            Properties properties = new Properties();
            properties.load(in);
            String nhctlVersion = properties.getProperty("nhctlVersion");

            Version v = Version.valueOf(version);

            if (!v.satisfies(nhctlVersion)) {
                NocalhostNotifier.getInstance(project).notifyVersionTips(nhctlVersion, version);
            }
        } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
            NocalhostNotifier.getInstance(project).notifyError("Get nhctl version error", e.getMessage());
        }
    }

    private void toggleContent() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        String jwt = nocalhostSettings.getJwt();

        panel.removeAll();

        if (StringUtils.isNotBlank(jwt)) {
            tree = new NocalhostTree(project);
            Disposer.register(this, tree);
            tree.updateDevSpaces();
            JBScrollPane scrollPane = new JBScrollPane(tree);
            scrollPane.setBorder(new TopLineBorder(new JBColor(0xD5D5D5, 0x323232), 1));
            panel.add(scrollPane);
        } else {
            if (tree != null) {
                Disposer.dispose(tree);
                tree = null;
            }
            panel.add(new NocalhostWindowLoginPanel().getPanel());
        }
        setToolbar();

        panel.repaint();
        panel.revalidate();
    }

    private void setToolbar() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        String jwt = nocalhostSettings.getJwt();
        if (StringUtils.isNotBlank(jwt)) {
            DefaultActionGroup moreActionGroup = new DefaultActionGroup();
            moreActionGroup.getTemplatePresentation().setText("More");
            moreActionGroup.getTemplatePresentation().setDescription("More");
            moreActionGroup.getTemplatePresentation().setIcon(AllIcons.Actions.More);
            moreActionGroup.setPopup(true);
            moreActionGroup.add(new SettingAction());
            moreActionGroup.add(new LogoutAction());

            DefaultActionGroup actionGroup = new DefaultActionGroup();
            actionGroup.add(new RefreshAction());
            actionGroup.add(new Separator());
            actionGroup.add(moreActionGroup);

            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Nocalhost.Toolbar", actionGroup, true);
            panel.setToolbar(actionToolbar.getComponent());
        } else {
            if (panel.getToolbar() != null) {
                panel.setToolbar(null);
            }
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void dispose() {

    }
}
