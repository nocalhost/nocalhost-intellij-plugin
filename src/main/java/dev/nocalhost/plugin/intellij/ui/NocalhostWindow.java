package dev.nocalhost.plugin.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
import dev.nocalhost.plugin.intellij.ui.action.LogoutAction;
import dev.nocalhost.plugin.intellij.ui.action.RefreshAction;
import dev.nocalhost.plugin.intellij.ui.action.SettingAction;
import dev.nocalhost.plugin.intellij.ui.tree.NocalhostTree;
import dev.nocalhost.plugin.intellij.service.NocalhostBinService;
import dev.nocalhost.plugin.intellij.utils.TokenUtil;

public class NocalhostWindow implements Disposable {
    private static final Logger LOG = Logger.getInstance(NocalhostWindow.class);

    private final Project project;

    private final SimpleToolWindowPanel panel;
    private NocalhostTree tree;

    public NocalhostWindow(Project project) {
        this.project = project;

        checkNocalhost();

        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect(this).subscribe(
                NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC,
                NocalhostWindow.this::toggleContent
        );

        panel = new SimpleToolWindowPanel(true, false);

        toggleContent();
    }

    private void checkNocalhost() {
        NocalhostBinService nocalhostBinService = new NocalhostBinService(project);
        nocalhostBinService.checkBin();
        nocalhostBinService.checkVersion();
    }

    private void toggleContent() {
        panel.removeAll();

        if (TokenUtil.isTokenValid()) {
            tree = new NocalhostTree(project);
            new TreeSpeedSearch(tree);
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
        if (TokenUtil.isTokenValid()) {
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
