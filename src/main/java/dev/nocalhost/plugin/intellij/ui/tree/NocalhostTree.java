package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeSelectionModel;

import dev.nocalhost.plugin.intellij.topic.NocalhostTreeDataUpdateNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUiUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;

public class NocalhostTree extends Tree implements Disposable {
    private final Project project;
    private final NocalhostTreeModel model;

    public NocalhostTree(Project project) {
        this.project = project;
        this.model = new NocalhostTreeModel(project);
        this.setModel(this.model);

        init();
    }

    private void init() {
        this.setRootVisible(false);
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setCellRenderer(new TreeNodeRenderer());
        this.addMouseListener(new TreeMouseListener(this, project));
        this.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object node = event.getPath().getLastPathComponent();
                if (node instanceof ResourceTypeNode) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) node;
                    resourceTypeNode.setLoaded(true);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {

            }
        });
        this.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                Object node = event.getPath().getLastPathComponent();
                if (node instanceof ResourceTypeNode) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) node;
                    model.updateResources(resourceTypeNode);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {

            }
        });

        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
                NocalhostTreeUiUpdateNotifier.NOCALHOST_TREE_UI_UPDATE_NOTIFIER_TOPIC,
                model::update
        );
    }

    public void updateDevSpaces() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeDataUpdateNotifier.NOCALHOST_TREE_DATA_UPDATE_NOTIFIER_TOPIC
        ).action();
    }

    @Override
    public void dispose() {

    }
}