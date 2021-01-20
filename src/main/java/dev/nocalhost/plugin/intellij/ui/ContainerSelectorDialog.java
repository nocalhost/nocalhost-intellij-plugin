package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

import javax.swing.*;

public class ContainerSelectorDialog extends DialogWrapper {

    private final JPanel panel;
    private ComboBox<String> comboBox;

    private String current;

    public ContainerSelectorDialog(String[] list) {
        super(true);
        setModal(true);

        setTitle("Select Pod");

        panel = new JPanel(new GridLayout(2, 1));
        comboBox = new ComboBox<>(list);
        comboBox.addItemListener(e -> {
            if (e.getSource() == comboBox) {
                current = (String) comboBox.getSelectedItem();
            }
        });
        panel.add(comboBox);
        comboBox.setSelectedIndex(0);
        current = list[0];
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    @Override
    public void doCancelAction() {
        current = "";
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    public String getCurrent() {
        return current;
    }

}
