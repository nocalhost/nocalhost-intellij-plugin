package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.ide.plugins.newui.ColorButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBEmptyBorder;
import com.intellij.util.ui.UIUtil;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.CompoundBorder;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlAppPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardListOptions;
import dev.nocalhost.plugin.intellij.ui.VerticalFlowLayout;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import lombok.SneakyThrows;

public class AppPortForwardConfigurationDialog extends DialogWrapper {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;


    private final Project project;
    private final ApplicationNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private final JPanel dialogPanel;
    private JPanel listPanel;

    public AppPortForwardConfigurationDialog(Project project, ApplicationNode node) {
        super(project);
        setTitle("Port forward configuration for application " + node.getName());

        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);

        dialogPanel = new SimpleToolWindowPanel(true);
        dialogPanel.setMinimumSize(new Dimension(400, -1));

        setupStopPanel();

        init();

        updatePortForwardList();
    }

    @Override
    protected Action @NotNull [] createActions() {
        myCancelAction.putValue(Action.NAME, "Close");
        return new Action[]{getCancelAction()};
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    private void updatePortForwardList() {
        ProgressManager.getInstance().run(new Task.Modal(project, "Loading Port Forward List", false) {
            private List<NhctlAppPortForward> devPortForwardList;

            @Override
            public void onSuccess() {
                super.onSuccess();
                createList(devPortForwardList);
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, "Nocalhost port forward error",
                        "Error occurred while loading port forward list", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlPortForwardListOptions opts = new NhctlPortForwardListOptions(kubeConfigPath, namespace);
                devPortForwardList = nhctlCommand.listPortForward(node.getName(), opts);
            }
        });
    }

    private void setupStopPanel() {
        listPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true));

        JScrollPane scrollPane = new JBScrollPane(listPanel);
        scrollPane.setPreferredSize(new Dimension(-1, 400));

        GridConstraints scrollPaneConstraints = new GridConstraints();
        scrollPaneConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW);
        scrollPaneConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        scrollPaneConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        dialogPanel.add(scrollPane);
    }

    private void createList(List<NhctlAppPortForward> portForwards) {
        final List<NhctlAppPortForward> ports = portForwards.stream()
                .filter(pf -> !StringUtils.equalsIgnoreCase(pf.getRole(), "SYNC"))
                .sorted(Comparator.comparing(NhctlAppPortForward::getPort))
                .collect(Collectors.toList());
        List<JPanel> items = Lists.newArrayList();

        for (NhctlAppPortForward portForward : ports) {
            items.add(createItem(portForward));
        }

        listPanel.removeAll();
        for (JPanel item : items) {
            listPanel.add(item);
        }
        listPanel.repaint();
        listPanel.revalidate();
    }

    private JPanel createItem(NhctlAppPortForward portForward) {
        JLabel label = new JLabel(String.format("%s (%s,%s)", portForward.getPort(), portForward.getServiceName(), portForward.getStatus()));
        label.setBorder(new CompoundBorder(new JBEmptyBorder(0), new JBEmptyBorder(0, 8, 0, 8)));
        GridConstraints labelConstraints = new GridConstraints();
        labelConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW);
        labelConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        labelConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        labelConstraints.setColumn(0);

        JButton button = new StopButton();
        button.addActionListener(event -> {
            if (!MessageDialogBuilder.yesNo("Port forward", "Stop port forward " + portForward.getPort() + " (" + portForward.getServiceName() + ")?").ask(project)) {
                return;
            }

            String sudoPassword = null;
            if (SystemInfo.isLinux && NumberUtils.toInt(portForward.getPort().split(":")[0]) < 1024) {
                SudoPasswordDialog sudoPasswordDialog = new SudoPasswordDialog(project, NhctlUtil.binaryPath());
                if (sudoPasswordDialog.showAndGet()) {
                    sudoPassword = sudoPasswordDialog.getPassword();
                } else {
                    return;
                }
            }

            final String finalSudoPassword = sudoPassword;
            ProgressManager.getInstance().run(new Task.Modal(project, "Stopping port forward " + portForward, false) {
                @Override
                public void onThrowable(@NotNull Throwable e) {
                    ErrorUtil.dealWith(project, "Nocalhost port forward error", "Error occurred while stopping port forward", e);
                }

                @Override
                public void onFinished() {
                    updatePortForwardList();
                }

                @SneakyThrows
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    NhctlPortForwardEndOptions opts = new NhctlPortForwardEndOptions(kubeConfigPath, namespace);
                    opts.setPort(portForward.getPort());
                    opts.setDeployment(portForward.getServiceName());
                    opts.setType(portForward.getServiceType());
                    outputCapturedNhctlCommand.endPortForward(node.getName(), opts, finalSudoPassword);
                }
            });
        });
        GridConstraints buttonConstraints = new GridConstraints();
        buttonConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW);
        buttonConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        buttonConstraints.setFill(GridConstraints.FILL_NONE);
        buttonConstraints.setColumn(1);

        GridLayoutManager itemLayoutManager = new GridLayoutManager(1, 2);
        itemLayoutManager.setSameSizeHorizontally(false);
        itemLayoutManager.setSameSizeVertically(false);
        itemLayoutManager.setHGap(-1);
        itemLayoutManager.setVGap(-1);

        JPanel panel = new JPanel();
        panel.setLayout(itemLayoutManager);
        panel.add(label, labelConstraints);
        panel.add(button, buttonConstraints);

        return panel;
    }

    private static class StopButton extends ColorButton {
        private final Color RedColor = new JBColor(0xC06362, 0xAC5D52);
        private final Color BackgroundColor = new JBColor(() -> JBColor.isBright() ? UIUtil.getListBackground() : new Color(0x313335));
        private final Color ForegroundColor = RedColor;
        private final Color BorderColor = RedColor;
        private final Color FocusedBackground = new Color(0xF1BAC5);

        public StopButton() {
            setTextColor(ForegroundColor);
            setFocusedTextColor(ForegroundColor);
            setBgColor(BackgroundColor);
            setFocusedBgColor(FocusedBackground);
            setBorderColor(BorderColor);
            setFocusedBorderColor(BorderColor);

            setText("Stop");
            setWidth72(this);
        }
    }
}
