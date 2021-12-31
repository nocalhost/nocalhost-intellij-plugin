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
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeContext;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.TextUiUtil;
import lombok.SneakyThrows;

public class AddStandaloneClustersDialog extends DialogWrapper {
    private final Project project;

    private JPanel dialogPanel;
    private JLabel lblMessage;
    private JBTextField txtNamespace;
    private JComboBox<KubeContext> cmbContexts;
    private JTabbedPane tabbedPane;
    private TextFieldWithBrowseButton kubeconfigFileSelectTextField;
    private JBTextArea kubeconfigFilePasteTextField;

    public AddStandaloneClustersDialog(Project project) {
        super(project, true);
        this.project = project;

        setTitle("Connect to Cluster");
        setOKButtonText("Add");

        txtNamespace.getEmptyText().setText("Type in a correct namespace");
        cmbContexts.setRenderer(new KubeContextRender());
        cmbContexts.addItemListener(e -> {
            if (ItemEvent.SELECTED == e.getStateChange()) {
                var item = (KubeContext) e.getItem();
                txtNamespace.setText(item.getContext().getNamespace());
            }
        });

        tabbedPane.addChangeListener(e -> {
            switch (tabbedPane.getSelectedIndex()) {
                case 0:
                    setContextsForKubeconfigFileSelectTextField();
                    break;
                case 1:
                    setContextsForKubeconfigFilePasteTextField();
                    break;
                default:
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
                    return new ValidationInfo("Please select KubeConfig file",
                            kubeconfigFileSelectTextField);
                }
                break;
            case 1:
                if (!StringUtils.isNotEmpty(kubeconfigFilePasteTextField.getText())) {
                    return new ValidationInfo("Please paste KubeConfig text",
                            kubeconfigFilePasteTextField);
                }
                break;
            default:
                break;
        }
        if (cmbContexts.getSelectedIndex() == -1) {
            return new ValidationInfo("Context is required", cmbContexts);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new Task.Modal(project, "Adding", false) {
            @Override
            public void onSuccess() {
                super.onSuccess();
                AddStandaloneClustersDialog.super.doOKAction();
            }

            @Override
            public void onThrowable(@NotNull Throwable ex) {
                ErrorUtil.dealWith(project, "Failed to add clusters",
                        "Error occurred while adding clusters", ex);
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
//                var config = getRawKubeConfig();
//                var contexts = contextList.getSelectedValuesList();
//                // check and show warning
//                var cmd = new NhctlKubeConfigCheckCommand(project);
//                cmd.setKubeConfig(KubeConfigUtil.kubeConfigPath(config));
//                cmd.setContexts(contexts.stream().map(KubeContext::getName).collect(Collectors.toList()));
//                cmd.execute();
//                // https://nocalhost.coding.net/p/nocalhost/subtasks/issues/702/detail
//                ProgressManager.getInstance().run(new AddStandaloneClusterTask(project, config, contexts));
            }
        });
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
        cmbContexts.removeAllItems();
        String text = kubeconfigFileSelectTextField.getText();
        if (StringUtils.isNotEmpty(text)) {
            var contexts = resolveContexts(Paths.get(text));
            contexts.forEach(x -> cmbContexts.addItem(x));

            if (contexts.size() == 1) {
                cmbContexts.setSelectedIndex(0);
            }
        }
    }

    private void setContextsForKubeconfigFilePasteTextField() {
        cmbContexts.removeAllItems();
        String text = kubeconfigFilePasteTextField.getText();
        if (StringUtils.isNotEmpty(text)) {
            var contexts = resolveContexts(text);
            contexts.forEach(x -> cmbContexts.addItem(x));

            if (contexts.size() == 1) {
                cmbContexts.setSelectedIndex(0);
            }
        }
    }

    private static class KubeContextRender extends JLabel implements ListCellRenderer<KubeContext> {

        public KubeContextRender() {
            super();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends KubeContext> list, KubeContext value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getName());
            return this;
        }
    }
}
