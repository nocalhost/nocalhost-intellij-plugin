package dev.nocalhost.plugin.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.StatusText;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.ui.action.AddStandaloneClustersAction;
import dev.nocalhost.plugin.intellij.ui.action.CleanConfigurationAction;
import dev.nocalhost.plugin.intellij.ui.action.ConnectNocalhostApiServerAction;
import dev.nocalhost.plugin.intellij.ui.action.LocateCurrentServiceAction;
import dev.nocalhost.plugin.intellij.ui.action.ManageNocalhostAccountsAction;
import dev.nocalhost.plugin.intellij.ui.dialog.AddStandaloneClustersDialog;
import dev.nocalhost.plugin.intellij.ui.action.RefreshAction;
import dev.nocalhost.plugin.intellij.ui.tree.NocalhostTree;

public class NocalhostWindow implements Disposable {

    private final Project project;

    private final SimpleToolWindowPanel panel;
    private NocalhostTree tree;

    public NocalhostWindow(Project project) {
        this.project = project;

        panel = new SimpleToolWindowPanel(true, false);

        init();
    }

    private void init() {
        tree = new NocalhostTree(project);
        TreeSpeedSearch.installOn(tree);
        tree.updateDevSpaces();
        StatusText emptyText = tree.getEmptyText();
        emptyText.setCenterAlignText(false);
        emptyText.appendLine("Get started with Nocalhost by connecting to a Kubernetes cluster.");
        emptyText.appendLine("connect to a cluster", SimpleTextAttributes.LINK_ATTRIBUTES,
                event -> new AddStandaloneClustersDialog(project).showAndGet());
        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setBorder(new TopLineBorder(new JBColor(0xD5D5D5, 0x323232), 1));
        panel.add(scrollPane);

        setToolbar();
    }

    private void setToolbar() {
        DefaultActionGroup moreActionGroup = new DefaultActionGroup();
        moreActionGroup.getTemplatePresentation().setText("More");
        moreActionGroup.getTemplatePresentation().setDescription("More");
        moreActionGroup.getTemplatePresentation().setIcon(AllIcons.General.Settings);
        moreActionGroup.setPopup(true);
        moreActionGroup.add(new CleanConfigurationAction(project));

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AddStandaloneClustersAction(project));
        actionGroup.add(new Separator());
        actionGroup.add(new ConnectNocalhostApiServerAction(project));
        actionGroup.add(new ManageNocalhostAccountsAction(project));
        actionGroup.add(new Separator());
        actionGroup.add(new RefreshAction());
        actionGroup.add(new LocateCurrentServiceAction(project));
        actionGroup.add(new Separator());
        actionGroup.add(moreActionGroup);

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Nocalhost.Toolbar", actionGroup, true);
        actionToolbar.setTargetComponent(panel);
        panel.setToolbar(actionToolbar.getComponent());
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void dispose() {
        Disposer.dispose(tree);
    }
}
