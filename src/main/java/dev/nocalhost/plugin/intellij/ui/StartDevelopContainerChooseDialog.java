package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;

import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;

public class StartDevelopContainerChooseDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JBList<ServiceContainer> containerList;

    private ServiceContainer selectedContainer;

    public StartDevelopContainerChooseDialog(List<ServiceContainer> containers) {
        super(true);

        setTitle("Select Container");

        init();

        containerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        containerList.setCellRenderer(new ContainerItemRenderer());
        containerList.setListData(containers.toArray(new ServiceContainer[0]));
    }

    @Override
    protected void doOKAction() {
        if (containerList.getSelectedValuesList().size() > 0) {
            selectedContainer = containerList.getSelectedValuesList().get(0);
        }
        super.doOKAction();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    public ServiceContainer getSelectedContainer() {
        return selectedContainer;
    }

    private class ContainerItemRenderer implements ListCellRenderer<ServiceContainer> {

        @Override
        public Component getListCellRendererComponent(
                JList<? extends ServiceContainer> list,
                ServiceContainer value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            return new JLabel(value.getName());
        }
    }
}
