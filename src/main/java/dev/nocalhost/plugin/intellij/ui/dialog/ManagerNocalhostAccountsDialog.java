package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBList;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.task.LogoutNocalhostAccountsTask;
import dev.nocalhost.plugin.intellij.utils.TokenUtil;

public class ManagerNocalhostAccountsDialog extends DialogWrapper {
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(
            NocalhostSettings.class);

    private final Project project;

    private JPanel contentPane;
    private JBList<NocalhostAccount> accountList;

    public ManagerNocalhostAccountsDialog(Project project) {
        super(project, true);
        this.project = project;

        setOKButtonText("Logout");

        accountList.setCellRenderer(new ListItemCheckBox());
        accountList.setVisibleRowCount(20);
        accountList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
                    super.addSelectionInterval(index0, index1);
                }
            }
        });
        accountList.setListData(nocalhostSettings.getNocalhostAccounts().toArray(
                new NocalhostAccount[0]));

        init();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (accountList.getSelectedValuesList().size() == 0) {
            return new ValidationInfo("Must select at least ONE account", accountList);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new LogoutNocalhostAccountsTask(project,
                accountList.getSelectedValuesList()));
        super.doOKAction();
    }

    private static class ListItemCheckBox extends JCheckBox
            implements ListCellRenderer<NocalhostAccount> {
        public ListItemCheckBox() {
            super();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends NocalhostAccount> list,
                                                      NocalhostAccount value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            this.setText(value.getUsername() + " on " + value.getServer() + " before "
                    + TokenUtil.expiredAt(value.getJwt()));
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            this.setSelected(isSelected);
            return this;
        }
    }
}
