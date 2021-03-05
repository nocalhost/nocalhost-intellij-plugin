package dev.nocalhost.plugin.intellij.ui;

import com.intellij.ide.plugins.newui.ColorButton;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBEmptyBorder;
import com.intellij.util.ui.UIUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.CompoundBorder;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ExecutableUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class PortForwardConfigurationDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(PortForwardConfigurationDialog.class);

    private final ResourceNode node;
    private final Project project;

    private JPanel dialogPanel;
    private JScrollPane scrollPane;
    private JPanel listPanel;

    private JBTextField startTextField;
    private JButton startButton;

    public PortForwardConfigurationDialog(ResourceNode node, Project project) {
        super(true);
        setTitle("Port forward configuration for service " + node.resourceName());

        this.node = node;
        this.project = project;

        dialogPanel = new SimpleToolWindowPanel(true);

        setupStartPanel();
        setupStopPanel();

        init();

        updatePortForwardList();
    }

    @Override
    protected Action @NotNull [] createActions() {
        myOKAction.putValue(Action.NAME, "Close");
        return new Action[]{getOKAction()};
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    private void updatePortForwardList() {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

        NhctlDescribeOptions opts = new NhctlDescribeOptions();
        opts.setDeployment(node.resourceName());
        if (node.getKubeResource().getKind().equalsIgnoreCase("statefulset")) {
            opts.setType("statefulset");
        }
        opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());

        ProgressManager.getInstance().run(new Task.Modal(project, "Loading Port Forward List", false) {
            private List<NhctlPortForward> devPortForwardList;


            @Override
            public void onSuccess() {
                super.onSuccess();
                createList(devPortForwardList);
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                            node.devSpace().getContext().getApplicationName(),
                            opts,
                            NhctlDescribeService.class);
                    devPortForwardList = nhctlDescribeService.getDevPortForwardList();
                } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                    LOG.error("error occurred while loading port forward list", e);
                    NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while loading port forward list", e.getMessage());
                }
            }
        });
    }

    private void setupStartPanel() {
        startTextField = new JBTextField();
        startTextField.getEmptyText().appendText("single: 1234:1234, multiple: 1234:1234,5678:5678");
        startTextField.setMinimumSize(new Dimension(400, -1));
        startTextField.addCaretListener(e -> startButton.setEnabled(StringUtils.isNotEmpty(startTextField.getText())));
        GridConstraints textFieldConstraints = new GridConstraints();
        textFieldConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW);
        textFieldConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_CAN_SHRINK);
        textFieldConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        textFieldConstraints.setColumn(0);

        startButton = new StartButton();
        startButton.setEnabled(false);
        startButton.addActionListener(event -> {
            Set<String> portForwardsToBeStarted = Arrays.stream(startTextField.getText().split(",")).map(String::trim).collect(Collectors.toSet());

            AtomicReference<String> sudoPassword = new AtomicReference<>(null);
            if (SystemInfo.isLinux && hasPrivilegedPorts(List.copyOf(portForwardsToBeStarted))) {
                SudoPasswordDialog sudoPasswordDialog = new SudoPasswordDialog(project, ExecutableUtil.lookup("nhctl"));
                if (!sudoPasswordDialog.showAndGet()) {
                    return;
                }
                sudoPassword.set(sudoPasswordDialog.getPassword());
            }

            final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
            String container = null;
            KubeResourceList pods = null;
            try {
                pods = kubectlCommand.getResourceList("pods", node.getKubeResource().getSpec().getSelector().getMatchLabels(), node.devSpace());
            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "List Resource error while starting port forward", e.getMessage());
            }
            if (pods != null && CollectionUtils.isNotEmpty(pods.getItems())) {
                List<String> containers = pods.getItems().stream().map(r -> r.getMetadata().getName()).collect(Collectors.toList());
                container = selectContainer(containers);
            }
            if (StringUtils.isBlank(container)) {
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Resource not found while starting port forward");
                return;
            }
            String finalContainer = container;
            ProgressManager.getInstance().run(new Task.Modal(project, "Starting port forward " + startTextField.getText(), true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                    try {
                        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions();
                        nhctlDescribeOptions.setDeployment(node.resourceName());
                        if (node.getKubeResource().getKind().equalsIgnoreCase("statefulset")) {
                            nhctlDescribeOptions.setType("statefulset");
                        }
                        nhctlDescribeOptions.setKubeconfig(
                                KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());
                        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                                node.devSpace().getContext().getApplicationName(),
                                nhctlDescribeOptions,
                                NhctlDescribeService.class);
                        List<String> existedPortForwards = nhctlDescribeService
                                .getDevPortForwardList()
                                .stream()
                                .map(NhctlPortForward::portForwardStr)
                                .collect(Collectors.toList());

                        portForwardsToBeStarted.removeAll(existedPortForwards);
                        if (portForwardsToBeStarted.size() > 0) {
                            final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
                            NhctlPortForwardStartOptions nhctlPortForwardStartOptions = new NhctlPortForwardStartOptions();
                            nhctlPortForwardStartOptions.setDevPorts(Lists.newArrayList(portForwardsToBeStarted.iterator()));
                            nhctlPortForwardStartOptions.setWay(NhctlPortForwardStartOptions.Way.MANUAL);
                            nhctlPortForwardStartOptions.setDeployment(node.resourceName());
                            if (node.getKubeResource().getKind().equalsIgnoreCase("statefulset")) {
                                nhctlPortForwardStartOptions.setType("statefulset");
                            }
                            nhctlPortForwardStartOptions.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());

                            nhctlPortForwardStartOptions.setPod(finalContainer);


                            outputCapturedNhctlCommand.startPortForward(node.devSpace().getContext().getApplicationName(), nhctlPortForwardStartOptions, sudoPassword.get());
                        }
                    } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                        LOG.error("error occurred while starting port forward", e);
                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while starting port forward", e.getMessage());
                    } finally {
                        updatePortForwardList();
                        startTextField.setText("");
                        startButton.setEnabled(false);
                    }
                }
            });
        });
        GridConstraints buttonConstraints = new GridConstraints();
        buttonConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW);
        buttonConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        buttonConstraints.setFill(GridConstraints.FILL_NONE);
        buttonConstraints.setColumn(1);

        GridLayoutManager startPanelLayoutManager = new GridLayoutManager(1, 2);
        startPanelLayoutManager.setSameSizeHorizontally(false);
        startPanelLayoutManager.setSameSizeVertically(false);
        startPanelLayoutManager.setHGap(-1);
        startPanelLayoutManager.setVGap(-1);

        JPanel startPanel = new JPanel();
        startPanel.setLayout(startPanelLayoutManager);
        startPanel.add(startTextField, textFieldConstraints);
        startPanel.add(startButton, buttonConstraints);
        startPanel.setBorder(new CompoundBorder(new JBEmptyBorder(0), new JBEmptyBorder(0, 0, 10, 15)));

        GridConstraints stopPanelConstraints = new GridConstraints();
        stopPanelConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        stopPanelConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        stopPanelConstraints.setFill(GridConstraints.FILL_BOTH);
        dialogPanel.add(startPanel, BorderLayout.NORTH);
    }

    private void setupStopPanel() {
        listPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true));

        scrollPane = new JBScrollPane(listPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(-1, 400));

        GridConstraints scrollPaneConstraints = new GridConstraints();
        scrollPaneConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW);
        scrollPaneConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        scrollPaneConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        dialogPanel.add(scrollPane);
    }

    private void createList(List<NhctlPortForward> portForwards) {
        List<JPanel> items = Lists.newArrayList();

        for (NhctlPortForward portForward : portForwards) {
            items.add(createItem(portForward));
        }

        listPanel.removeAll();
        for (JPanel item : items) {
            listPanel.add(item);
        }
        listPanel.repaint();
        listPanel.revalidate();
    }

    private JPanel createItem(NhctlPortForward portForward) {
        JLabel label = new JLabel(portForward.portForward());
        label.setBorder(new CompoundBorder(new JBEmptyBorder(0), new JBEmptyBorder(0, 8, 0, 8)));
        GridConstraints labelConstraints = new GridConstraints();
        labelConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW);
        labelConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        labelConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        labelConstraints.setColumn(0);

        JButton button = new StopButton();
        button.addActionListener(event -> {
            if (!MessageDialogBuilder.yesNo("Port forward", "Stop port forward " + portForward.portForwardStr() + "?").guessWindowAndAsk()) {
                return;
            }

            AtomicReference<String> sudoPassword = new AtomicReference<>(null);
            try {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                NhctlDescribeOptions opts = new NhctlDescribeOptions();
                opts.setDeployment(node.resourceName());
                if (node.getKubeResource().getKind().equalsIgnoreCase("statefulset")) {
                    opts.setType("statefulset");
                }
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.devSpace().getContext().getApplicationName(),
                        opts,
                        NhctlDescribeService.class);

                Optional<NhctlPortForward> currentPortForward = nhctlDescribeService
                        .getDevPortForwardList()
                        .stream()
                        .filter(e -> StringUtils.equals(portForward.getLocalport(), e.getLocalport())
                                && StringUtils.equals(portForward.getRemoteport(), e.getRemoteport()))
                        .findFirst();
                if (!currentPortForward.isPresent()) {
                    return;
                }

                List<String> portsHostedBySameProcess = nhctlDescribeService
                        .getDevPortForwardList()
                        .stream()
                        .filter(e -> e.getPid().equals(currentPortForward.get().getPid()))
                        .map(NhctlPortForward::portForwardStr)
                        .collect(Collectors.toList());
                if (SystemInfo.isLinux && hasPrivilegedPorts(portsHostedBySameProcess)) {
                    SudoPasswordDialog sudoPasswordDialog = new SudoPasswordDialog(project, ExecutableUtil.lookup("nhctl"));
                    if (!sudoPasswordDialog.showAndGet()) {
                        return;
                    }
                    sudoPassword.set(sudoPasswordDialog.getPassword());
                }
            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                LOG.error("error occurred while checking port forward before stopping", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while checking port forward before stopping", e.getMessage());
                return;
            } finally {
                updatePortForwardList();
            }

            ProgressManager.getInstance().run(new Task.Modal(project, "Stopping port forward " + portForward, false) {
                @Override
                public void onThrowable(@NotNull Throwable e) {
                    LOG.error("error occurred while stopping port forward", e);
                    NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while stopping port forward", e.getMessage());
                }

                @Override
                public void onFinished() {
                    updatePortForwardList();
                }

                @SneakyThrows
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);

                    NhctlPortForwardEndOptions opts = new NhctlPortForwardEndOptions();
                    opts.setPort(portForward.portForwardStr());
                    opts.setDeployment(node.resourceName());
                    if (node.getKubeResource().getKind().equalsIgnoreCase("statefulset")) {
                        opts.setType("statefulset");
                    }
                    opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());

                    outputCapturedNhctlCommand.endPortForward(node.devSpace().getContext().getApplicationName(), opts, sudoPassword.get());
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

    private String selectContainer(List<String> containers) {
        if (containers.size() > 1) {
            StartDevelopContainerChooseDialog dialog = new StartDevelopContainerChooseDialog(containers);
            if (dialog.showAndGet()) {
                return dialog.getSelectedContainer();
            } else {
                return null;
            }
        } else {
            return containers.get(0);
        }
    }

    private static class StartButton extends ColorButton {
        private final Color BlueColor = new JBColor(0x669ED5, 0x5E91C3);
        private final Color BackgroundColor = new JBColor(() -> JBColor.isBright() ? UIUtil.getListBackground() : new Color(0x313335));
        private final Color ForegroundColor = BlueColor;
        private final Color BorderColor = BlueColor;
        private final Color FocusedBackground = new Color(0xBEDBFD);

        public StartButton() {
            setTextColor(ForegroundColor);
            setFocusedTextColor(ForegroundColor);
            setBgColor(BackgroundColor);
            setFocusedBgColor(FocusedBackground);
            setBorderColor(BorderColor);
            setFocusedBorderColor(BorderColor);

            setText("Start");
            setWidth72(this);
        }
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

    private static boolean hasPrivilegedPorts(List<String> ports) {
        for (String port : ports) {
            if (isPrivilegedPort(port)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrivilegedPort(String port) {
        String[] splits = port.split(":", 2);
        String localPort = splits[0].trim();
        if (!StringUtils.isNotEmpty(localPort)) {
            return false;
        }
        try {
            return Integer.parseInt(localPort) < 1024;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
