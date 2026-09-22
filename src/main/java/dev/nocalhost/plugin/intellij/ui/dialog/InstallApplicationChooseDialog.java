package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;

import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.*;
import javax.swing.border.LineBorder;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;

public class InstallApplicationChooseDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JLabel chooseAppLabel;
    private JScrollPane scrollPane;
    private JBList<String> applicationList;

    private String selected;

    public InstallApplicationChooseDialog(List<String> apps) {
        super(true);

        setTitle(NocalhostI18n.get("dialog.chooseApplication"));
        chooseAppLabel.setText(NocalhostI18n.get("prompt.chooseApplicationToDeploy"));
        scrollPane.setBorder(new LineBorder(new JBColor(0xD5D5D5, 0x323232), 1));

        applicationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        applicationList.setListData(apps.toArray(new String[0]));
        applicationList.setSelectedIndex(0);
        applicationList.setVisibleRowCount(15);
        applicationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
                    doOKAction();
                }
            }
        });

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
