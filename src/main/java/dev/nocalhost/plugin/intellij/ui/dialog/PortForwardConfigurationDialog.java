package dev.nocalhost.plugin.intellij.ui.dialog;

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
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.CompoundBorder;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.ui.VerticalFlowLayout;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ExecutableUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.TextUiUtil;
import lombok.SneakyThrows;

public class PortForwardConfigurationDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(PortForwardConfigurationDialog.class);

    private static final Pattern PORT_TEXT_REGEX = Pattern.compile("^\\d+:\\d+(,\\d+:\\d+)*$");

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;


    private final ResourceNode node;
    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;

    private final JPanel dialogPanel;
    private JPanel listPanel;

    private JBTextField startTextField;
    private JButton startButton;

    public PortForwardConfigurationDialog(ResourceNode node, Project project) {
        super(project);
        setTitle("Port forward configuration for service " + node.resourceName());

        this.node = node;
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);

        dialogPanel = new SimpleToolWindowPanel(true);

        setupStartPanel();
        setupStopPanel();

        TextUiUtil.setCutCopyPastePopup(startTextField);

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
        ProgressManager.getInstance().run(new Task.Modal(project, "Loading Port Forward List", false) {
            private List<NhctlPortForward> devPortForwardList;

            @Override
            public void onSuccess() {
                super.onSuccess();
                createList(devPortForwardList);
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                LOG.error("error occurred while loading port forward list", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while loading port forward list", e.getMessage());
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(),
                        opts,
                        NhctlDescribeService.class);
                devPortForwardList = nhctlDescribeService.getDevPortForwardList();
            }
        });
    }

    private void setupStartPanel() {
        startTextField = new JBTextField();
        startTextField.getEmptyText().appendText("single: 1234:1234, multiple: 1234:1234,5678:5678");
        startTextField.setMinimumSize(new Dimension(400, -1));
        startTextField.addCaretListener(e -> {
            boolean enabled = false;
            String text = startTextField.getText();
            final Matcher matcher = PORT_TEXT_REGEX.matcher(text);
            enabled = matcher.matches();
            if (enabled) {
                Set<String> portForwardsToBeStarted = Arrays.stream(startTextField.getText().split(",")).map(String::trim).collect(Collectors.toSet());
                for (String next : portForwardsToBeStarted) {
                    final String[] split = next.split(":");
                    int split1 = NumberUtils.toInt(split[0]);
                    int split2 = NumberUtils.toInt(split[1]);
                    if (split1 <= 0 || split1 >= 65535 || split2 <= 0 || split2 >= 65535) {
                        enabled = false;
                        break;
                    }
                }
            }

            startButton.setEnabled(enabled);
        });
        GridConstraints textFieldConstraints = new GridConstraints();
        textFieldConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW);
        textFieldConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_CAN_SHRINK);
        textFieldConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        textFieldConstraints.setColumn(0);

        startButton = new StartButton();
        startButton.setEnabled(false);
        startButton.addActionListener(event -> {
            Set<String> portForwardsToBeStarted = Arrays.stream(startTextField.getText().split(",")).map(String::trim).collect(Collectors.toSet());

            String container = null;
            List<KubeResource> pods = null;
            try {
                NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, namespace);
                List<NhctlGetResource> podList = nhctlCommand.getResources("Pods", nhctlGetOptions,
                        node.getKubeResource().getSpec().getSelector().getMatchLabels());
                pods = podList.stream()
                        .map(NhctlGetResource::getKubeResource)
                        .filter(KubeResource::canSelector)
                        .collect(Collectors.toList());
            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "List Resource error while starting port forward", e.getMessage());
            }
            if (CollectionUtils.isNotEmpty(pods)) {
                if (pods.size() > 0) {
                    List<String> containers = pods.stream()
                            .map(r -> r.getMetadata().getName())
                            .collect(Collectors.toList());
                    container = selectContainer(containers);
                }
            }
            if (StringUtils.isBlank(container)) {
//                NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Resource not found while starting port forward");
                return;
            }
            String finalContainer = container;

            AtomicBoolean again = new AtomicBoolean(true);
            AtomicBoolean passwordRequired = new AtomicBoolean(false);
            AtomicReference<String> sudoPassword = new AtomicReference<>(null);
            do {
                if (passwordRequired.get()) {
                    SudoPasswordDialog sudoPasswordDialog = new SudoPasswordDialog(project, ExecutableUtil.lookup("nhctl"));
                    if (sudoPasswordDialog.showAndGet()) {
                        sudoPassword.set(sudoPasswordDialog.getPassword());
                    } else {
                        return;
                    }
                }

                ProgressManager.getInstance().run(new Task.Modal(project, "Starting port forward " + startTextField.getText(), true) {
                    @Override
                    public void onSuccess() {
                        again.set(false);
                    }

                    @Override
                    public void onThrowable(@NotNull Throwable throwable) {
                        if (throwable instanceof NocalhostExecuteCmdException) {
                            NocalhostExecuteCmdException e = (NocalhostExecuteCmdException) throwable;
                            if (StringUtils.contains(e.getMessage(), "permission denied")) {
                                passwordRequired.set(true);
                            } else {
                                again.set(false);
                            }
                        }
                        LOG.error("error occurred while starting port forward", throwable);
                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while starting port forward", throwable.getMessage());
                    }

                    @Override
                    public void onFinished() {
                        updatePortForwardList();
                        startTextField.setText("");
                        startButton.setEnabled(false);
                    }

                    @SneakyThrows
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath, namespace);
                        nhctlDescribeOptions.setDeployment(node.resourceName());
                        nhctlDescribeOptions.setType(node.getKubeResource().getKind());
                        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                                node.applicationName(),
                                nhctlDescribeOptions,
                                NhctlDescribeService.class);
                        List<String> existedPortForwards = nhctlDescribeService
                                .getDevPortForwardList()
                                .stream()
                                .map(NhctlPortForward::portForwardStr)
                                .collect(Collectors.toList());

                        portForwardsToBeStarted.removeAll(existedPortForwards);
                        if (portForwardsToBeStarted.size() > 0) {
                            NhctlPortForwardStartOptions nhctlPortForwardStartOptions = new NhctlPortForwardStartOptions(kubeConfigPath, namespace);
                            nhctlPortForwardStartOptions.setDevPorts(Lists.newArrayList(portForwardsToBeStarted.iterator()));
                            nhctlPortForwardStartOptions.setWay(NhctlPortForwardStartOptions.Way.MANUAL);
                            nhctlPortForwardStartOptions.setDeployment(node.resourceName());
                            nhctlPortForwardStartOptions.setType(node.getKubeResource().getKind());
                            nhctlPortForwardStartOptions.setPod(finalContainer);

                            outputCapturedNhctlCommand.startPortForward(node.applicationName(), nhctlPortForwardStartOptions, sudoPassword.get());
                        }
                    }
                });
            } while (SystemInfo.isLinux && again.get());
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

        GridConstraints stopPanelConstraints = new GridConstraints();
        stopPanelConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        stopPanelConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        stopPanelConstraints.setFill(GridConstraints.FILL_BOTH);
        dialogPanel.add(startPanel, BorderLayout.NORTH);
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

    private void createList(List<NhctlPortForward> portForwards) {
        final List<NhctlPortForward> ports = portForwards.stream()
                .filter(pf -> !StringUtils.equalsIgnoreCase(pf.getRole(), "SYNC"))
                .collect(Collectors.toList());
        List<JPanel> items = Lists.newArrayList();

        for (NhctlPortForward portForward : ports) {
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

            AtomicBoolean again = new AtomicBoolean(true);
            AtomicBoolean passwordRequired = new AtomicBoolean(false);
            AtomicReference<String> sudoPassword = new AtomicReference<>(null);
            do {
                if (passwordRequired.get()) {
                    SudoPasswordDialog sudoPasswordDialog = new SudoPasswordDialog(project, ExecutableUtil.lookup("nhctl"));
                    if (sudoPasswordDialog.showAndGet()) {
                        sudoPassword.set(sudoPasswordDialog.getPassword());
                    } else {
                        return;
                    }
                }
                ProgressManager.getInstance().run(new Task.Modal(project, "Stopping port forward " + portForward, false) {
                    @Override
                    public void onSuccess() {
                        again.set(false);
                    }

                    @Override
                    public void onThrowable(@NotNull Throwable throwable) {
                        if (throwable instanceof NocalhostExecuteCmdException) {
                            NocalhostExecuteCmdException e = (NocalhostExecuteCmdException) throwable;
                            if (StringUtils.contains(e.getMessage(), "operation not permitted")) {
                                passwordRequired.set(true);
                            } else {
                                again.set(false);
                            }
                        }
                        LOG.error("error occurred while stopping port forward", throwable);
                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while stopping port forward", throwable.getMessage());
                    }

                    @Override
                    public void onFinished() {
                        updatePortForwardList();
                    }

                    @SneakyThrows
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        NhctlPortForwardEndOptions opts = new NhctlPortForwardEndOptions(kubeConfigPath, namespace);
                        opts.setPort(portForward.portForwardStr());
                        opts.setDeployment(node.resourceName());
                        opts.setType(node.getKubeResource().getKind());
                        outputCapturedNhctlCommand.endPortForward(node.applicationName(), opts, sudoPassword.get());
                    }
                });
            } while (SystemInfo.isLinux && again.get());
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
            ListChooseDialog dialog = new ListChooseDialog(project, "Select Container", containers);
            if (dialog.showAndGet()) {
                return dialog.getSelectedValue();
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
}
