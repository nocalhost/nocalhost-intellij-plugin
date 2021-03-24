package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.*;
import javax.swing.border.LineBorder;

public class InstallApplicationChooseDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JScrollPane scrollPane;
    private JBList<String> applicationList;

    private String selected;

    public InstallApplicationChooseDialog(List<String> apps) {
        super(true);
        setTitle("Select Application");
        scrollPane.setBorder(new LineBorder(new JBColor(0xD5D5D5, 0x323232), 1));

        applicationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        applicationList.setListData(apps.toArray(new String[0]));
        applicationList.setSelectedIndex(0);

        init();
    }

    @Override
    protected void doOKAction() {
        if (applicationList.getSelectedValuesList().size() > 0) {
            selected = applicationList.getSelectedValuesList().get(0);
        }
        super.doOKAction();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    public String getSelected() {
        return selected;
    }
}
