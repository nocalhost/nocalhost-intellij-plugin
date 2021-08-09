package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlCleanPVCOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPVCItem;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;

public class ClearPersistentDataDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(ClearPersistentDataDialog.class);

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;

    private JPanel contentPane;
    private JButton selectAllButton;
    private JButton clearAllButton;
    private JList<NhctlPVCItem> pvcList;

    public ClearPersistentDataDialog(Project project, Path kubeConfigPath, String namespace, List<NhctlPVCItem> nhctlPVCItems) {
        super(true);
        init();
        setTitle("Clear PVC");

        this.project = project;
        this.kubeConfigPath = kubeConfigPath;
        this.namespace = namespace;

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);

        this.setOKActionEnabled(false);

        selectAllButton.setFocusable(false);
        selectAllButton.addActionListener((e) -> {
            pvcList.setSelectedIndices(IntStream.range(0, nhctlPVCItems.size()).toArray());
        });

        clearAllButton.setFocusable(false);
        clearAllButton.setEnabled(false);
        clearAllButton.addActionListener((e) -> {
            pvcList.clearSelection();
        });

        pvcList.setVisibleRowCount(18);
        pvcList.setCellRenderer(new ListItemCheckBox());
        pvcList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
                    super.addSelectionInterval(index0, index1);
                }
            }
        });
        pvcList.addListSelectionListener((e) -> {
            if (pvcList.getSelectedIndices().length == 0) {
                selectAllButton.setEnabled(true);
                clearAllButton.setEnabled(false);
                ClearPersistentDataDialog.this.setOKActionEnabled(false);
            } else if (pvcList.getSelectedIndices().length == nhctlPVCItems.size()) {
                selectAllButton.setEnabled(false);
                clearAllButton.setEnabled(true);
                ClearPersistentDataDialog.this.setOKActionEnabled(true);
            } else {
                selectAllButton.setEnabled(true);
                clearAllButton.setEnabled(true);
                ClearPersistentDataDialog.this.setOKActionEnabled(true);
            }
        });
        pvcList.setListData(nhctlPVCItems.toArray(new NhctlPVCItem[0]));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new Task.Modal(null, "Clearing persistent data", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);

                List<NhctlPVCItem> nhctlPVCItems = pvcList.getSelectedValuesList();
                for (int i = 0; i < nhctlPVCItems.size(); i++) {
                    NhctlPVCItem item = nhctlPVCItems.get(i);

                    indicator.setFraction((i + 1.0) / nhctlPVCItems.size());
                    indicator.setText(String.format("Clearing [name: %s, storage_class: %s, status: %s, capacity: %s]",
                            item.getName(), item.getStorageClass(), item.getStatus(), item.getCapacity()));

                    NhctlCleanPVCOptions opts = new NhctlCleanPVCOptions(kubeConfigPath, namespace);
                    opts.setApp(item.getAppName());
                    opts.setController(item.getServiceName());
                    opts.setName(item.getName());
                    try {
                        outputCapturedNhctlCommand.cleanPVC(opts);
                    } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                        LOG.error("error occurred while clearing persistent data", e);
                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost clear persistent data error", "Error occurred while clearing persistent data", e.getMessage());
                    }
                }
            }
        });

        super.doOKAction();
    }

    private class ListItemCheckBox extends JCheckBox implements ListCellRenderer<NhctlPVCItem> {
        public ListItemCheckBox() {
            super();
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends NhctlPVCItem> list,
                NhctlPVCItem value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            this.setText(String.format("name: %s, storage_class: %s, status: %s, capacity: %s",
                    value.getName(), value.getStorageClass(), value.getStatus(), value.getCapacity()));
            //setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            this.setSelected(isSelected);
            return this;
        }
    }
}
