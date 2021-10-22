package dev.nocalhost.plugin.intellij.ui.dialog;

import com.google.common.collect.Lists;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextArea;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeContext;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.nhctl.NhctlKubeConfigCheckCommand;
import dev.nocalhost.plugin.intellij.task.AddStandaloneClusterTask;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.TextUiUtil;
import lombok.SneakyThrows;

public class AddStandaloneClustersDialog extends DialogWrapper {
    private final Project project;

    private JPanel dialogPanel;

    private JTabbedPane tabbedPane;
    private JBList<KubeContext> contextList;
    private TextFieldWithBrowseButton kubeconfigFileSelectTextField;
    private JBTextArea kubeconfigFilePasteTextField;

    public AddStandaloneClustersDialog(Project project) {
        super(project, true);
        this.project = project;

        setTitle("Connect to Cluster");
        setOKButtonText("Add");

        tabbedPane.addChangeListener(e -> {
            switch (tabbedPane.getSelectedIndex()) {
                case 0:
                    setContextsForKubeconfigFileSelectTextField();
                    break;
                case 1:
                    setContextsForKubeconfigFilePasteTextField();
                    break;
                default:
                    contextList.setListData(new KubeContext[0]);
                    break;
            }
        });

        kubeconfigFileSelectTextField.addBrowseFolderListener("Select kubeconfig file", "",
                null, FileChooseUtil.singleFileChooserDescriptor());
        kubeconfigFileSelectTextField.getTextField().getDocument().addDocumentListener(
                new DocumentAdapter() {
                    @Override
                    protected void textChanged(@NotNull DocumentEvent e) {
                        setContextsForKubeconfigFileSelectTextField();
                    }
                });

        kubeconfigFilePasteTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                setContextsForKubeconfigFilePasteTextField();
            }
        });

        contextList.setCellRenderer(new ListItemCheckBox());
        contextList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
                    super.addSelectionInterval(index0, index1);
                }
            }
        });

        TextUiUtil.setCutCopyPastePopup(
                kubeconfigFileSelectTextField.getTextField(),
                kubeconfigFilePasteTextField);

        try {
            Path defaultKubeConfig = Paths.get(System.getProperty("user.home"), ".kube/config");
            if (Files.exists(defaultKubeConfig)) {
                kubeconfigFileSelectTextField.setText(defaultKubeConfig.toString());
            }
        } catch (Exception ignore) {
        }

        init();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        switch (tabbedPane.getSelectedIndex()) {
            case 0:
                if (!StringUtils.isNotEmpty(kubeconfigFileSelectTextField.getText())) {
                    return new ValidationInfo("Must select kubeconfig file",
                            kubeconfigFileSelectTextField);
                }
                break;
            case 1:
                if (!StringUtils.isNotEmpty(kubeconfigFilePasteTextField.getText())) {
                    return new ValidationInfo("Must paste kubeconfig text",
                            kubeconfigFilePasteTextField);
                }
                break;
            default:
                break;
        }
        if (contextList.getSelectedValuesList().size() == 0) {
            return new ValidationInfo("Must select at least ONE context", contextList);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new Task.Modal(project, "Checking", false) {
            @Override
            public void onSuccess() {
                super.onSuccess();
                AddStandaloneClustersDialog.super.doOKAction();
            }

            @Override
            public void onThrowable(@NotNull Throwable ex) {
                ErrorUtil.dealWith(project, "Adding clusters error",
                        "Error occurs while adding clusters", ex);
            }

            private String getRawKubeConfig() throws IOException {
                if (tabbedPane.getSelectedIndex() == 0) {
                    return Files.readString(Paths.get(kubeconfigFileSelectTextField.getText()), StandardCharsets.UTF_8);
                }
                return kubeconfigFilePasteTextField.getText();
            }

            @Override
            @SneakyThrows
            public void run(@NotNull ProgressIndicator indicator) {
                String rawKubeConfig = getRawKubeConfig();
                List<KubeContext> contexts = contextList.getSelectedValuesList();;
                for (KubeContext context : contexts) {
                    if (!check(KubeConfigUtil.kubeConfigPath(rawKubeConfig), context)) {
                        return;
                    }
                }
                ProgressManager.getInstance().run(new AddStandaloneClusterTask(project, rawKubeConfig, contexts));
            }
        });
    }

    private boolean check(Path path, KubeContext context) throws IOException, NocalhostExecuteCmdException, InterruptedException {
        var cmd = new NhctlKubeConfigCheckCommand(project);
        cmd.setKubeConfig(path);
        cmd.setContext(context.getName());
        return StringUtils.isEmpty(cmd.execute());
    }

    @Override
    @NotNull
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    protected void doHelpAction() {
        BrowserUtil.browse("https://nocalhost.dev");
    }

    private List<KubeContext> resolveContexts(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return resolveContexts(content);
        } catch (Exception ignore) {
        }
        return Lists.newArrayList();
    }

    private List<KubeContext> resolveContexts(String text) {
        try {
            KubeConfig kubeConfig = DataUtils.YAML.loadAs(text, KubeConfig.class);
            return kubeConfig.getContexts();
        } catch (Exception ignore) {
        }
        return Lists.newArrayList();
    }

    private void setContextsForKubeconfigFileSelectTextField() {
        contextList.setListData(new KubeContext[0]);
        String text = kubeconfigFileSelectTextField.getText();
        if (StringUtils.isNotEmpty(text)) {
            List<KubeContext> contexts = resolveContexts(Paths.get(text));
            contextList.setListData(contexts.toArray(new KubeContext[0]));
            if (contexts.size() == 1) {
                contextList.setSelectedIndex(0);
            }
        }
    }

    private void setContextsForKubeconfigFilePasteTextField() {
        contextList.setListData(new KubeContext[0]);
        String text = kubeconfigFilePasteTextField.getText();
        if (StringUtils.isNotEmpty(text)) {
            List<KubeContext> contexts = resolveContexts(text);
            contextList.setListData(contexts.toArray(new KubeContext[0]));
            if (contexts.size() == 1) {
                contextList.setSelectedIndex(0);
            }
        }
    }

    private static class ListItemCheckBox extends JCheckBox
            implements ListCellRenderer<KubeContext> {
        public ListItemCheckBox() {
            super();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends KubeContext> list,
                                                      KubeContext value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            this.setText(value.getName());
            this.setSelected(isSelected);
            return this;
        }
    }
}
