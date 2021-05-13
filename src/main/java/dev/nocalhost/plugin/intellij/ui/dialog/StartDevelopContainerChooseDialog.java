package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.*;
import javax.swing.border.LineBorder;

public class StartDevelopContainerChooseDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JScrollPane scrollPane;
    private JBList<String> containerList;

    private String selectedContainerName;

    public StartDevelopContainerChooseDialog(List<String> containers) {
        super(true);

        setTitle("Select Container");

        scrollPane.setBorder(new LineBorder(new JBColor(0xD5D5D5, 0x323232), 1));

        containerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        containerList.setListData(containers.toArray(new String[0]));
        containerList.setSelectedIndex(0);

        init();
    }

    @Override
    protected void doOKAction() {
        if (containerList.getSelectedValuesList().size() > 0) {
            selectedContainerName = containerList.getSelectedValuesList().get(0);
        }
        super.doOKAction();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    public String getSelectedContainer() {
        return selectedContainerName;
    }
}
