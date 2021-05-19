package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.*;
import javax.swing.border.LineBorder;

import lombok.Getter;

public class ListChooseDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JScrollPane scrollPane;
    private JBList<String> containerList;

    @Getter
    private String selectedValue;

    public ListChooseDialog(Project project, String title, List<String> values) {
        super(project, true);

        setTitle(title);

        scrollPane.setBorder(new LineBorder(new JBColor(0xD5D5D5, 0x323232), 1));

        containerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        containerList.setListData(values.toArray(new String[0]));
        containerList.setSelectedIndex(0);
        containerList.addMouseListener(new MouseAdapter() {
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
        selectedValue = containerList.getSelectedValue();
        super.doOKAction();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!StringUtils.isNotEmpty(containerList.getSelectedValue())) {
            return new ValidationInfo("Must select one item", containerList);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }
}
