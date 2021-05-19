package dev.nocalhost.plugin.intellij.ui.dialog;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.TextUiUtil;
import lombok.Getter;

public class StartDevelopDialog extends DialogWrapper {
    private static final List<String> DEV_MODE_IMAGES = List.of(
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/java:11",
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/ruby:3.0",
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/node:14",
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/python:3.9",
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/golang:1.16",
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/perl:latest",
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/rust:latest",
            "codingcorp-docker.pkg.coding.net/nocalhost/dev-images/php:latest"
    );

    private final NhctlDescribeService nhctlDescribeService;

    private JPanel contentPane;
    private JCheckBox cloneFromGitRepositoryCheckBox;
    private TextFieldWithBrowseButton sourceDirectoryTextField;
    private JBTextField gitUrlTextField;
    private JLabel sourceDirectoryLabel;
    private JBList<String> containerList;
    private JBList<String> imageList;

    @Getter
    private Path sourceDirectory;
    @Getter
    private boolean cloneFromGit;
    @Getter
    private String gitUrl;
    @Getter
    private String selectedContainer;
    @Getter
    private String selectedImage;

    public StartDevelopDialog(@Nullable Project project,
                              List<String> containers,
                              NhctlDescribeService nhctlDescribeService) {
        super(project, true);
        this.nhctlDescribeService = nhctlDescribeService;

        setTitle("Start Develop");
        setOKButtonText("Start");

        if (StringUtils.isNotEmpty(nhctlDescribeService.getAssociate())) {
            sourceDirectoryTextField.setText(nhctlDescribeService.getAssociate());
        }

        sourceDirectoryTextField.addBrowseFolderListener("Select Directory", "",
                null, FileChooseUtil.singleDirectoryChooserDescriptor());

        gitUrlTextField.setEnabled(false);

        containerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        containerList.setListData(containers.toArray(new String[0]));
        if (nhctlDescribeService.getRawConfig().getContainers().size() == 1) {
            containerList.setSelectedIndex(0);
            updateGitUrl();
            updateImageList();
        }
        containerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateGitUrl();
                updateImageList();
            }
        });

        imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cloneFromGitRepositoryCheckBox.addChangeListener(e -> {
            if (cloneFromGitRepositoryCheckBox.isSelected()) {
                gitUrlTextField.setEnabled(true);
                sourceDirectoryLabel.setText("Parent directory for git repository:");
            } else {
                gitUrlTextField.setEnabled(false);
                sourceDirectoryLabel.setText("Source directory:");
            }
        });

        TextUiUtil.setCutCopyPastePopup(sourceDirectoryTextField.getTextField(), gitUrlTextField);

        init();
    }

    private void updateGitUrl() {
        gitUrlTextField.setText("");

        String selectedContainer = containerList.getSelectedValue();
        if (!StringUtils.isNotEmpty(selectedContainer)) {
            return;
        }

        String gitUrl = "";
        Optional<ServiceContainer> serviceContainerOptional = nhctlDescribeService.getRawConfig()
                .getContainers().stream()
                .filter(e -> StringUtils.equals(e.getName(), selectedContainer))
                .findFirst();
        if (serviceContainerOptional.isPresent()
                && serviceContainerOptional.get().getDev() != null) {
            gitUrl = serviceContainerOptional.get().getDev().getGitUrl();
        }
        if (!StringUtils.isNotEmpty(gitUrl)
                && nhctlDescribeService.getRawConfig().getContainers().size() == 1
                && StringUtils.equals(nhctlDescribeService.getRawConfig().getContainers().get(0).getName(), "")
                && nhctlDescribeService.getRawConfig().getContainers().get(0).getDev() != null) {
            gitUrl = nhctlDescribeService.getRawConfig().getContainers().get(0).getDev().getGitUrl();
        }

        gitUrlTextField.setText(gitUrl);
    }

    private void updateImageList() {
        imageList.setListData(new String[0]);

        String selectedContainer = containerList.getSelectedValue();
        if (!StringUtils.isNotEmpty(selectedContainer)) {
            return;
        }

        String selectedImage = "";
        Optional<ServiceContainer> serviceContainerOptional = nhctlDescribeService.getRawConfig()
                .getContainers().stream()
                .filter(e -> StringUtils.equals(e.getName(), selectedContainer))
                .findFirst();
        if (serviceContainerOptional.isPresent()
                && serviceContainerOptional.get().getDev() != null) {
            selectedImage = serviceContainerOptional.get().getDev().getImage();
        }
        if (!StringUtils.isNotEmpty(selectedImage)
                && nhctlDescribeService.getRawConfig().getContainers().size() == 1
                && StringUtils.equals(nhctlDescribeService.getRawConfig().getContainers().get(0).getName(), "")
                && nhctlDescribeService.getRawConfig().getContainers().get(0).getDev() != null) {
            selectedImage = nhctlDescribeService.getRawConfig().getContainers().get(0).getDev().getImage();
        }

        final String selectImageFinal = selectedImage;
        List<String> images = Lists.newArrayList(DEV_MODE_IMAGES);
        if (StringUtils.isNotEmpty(selectedImage)) {
            Optional<String> imageOptional = images.stream()
                    .filter(e -> StringUtils.equals(e, selectImageFinal)).findFirst();
            if (imageOptional.isEmpty()) {
                images.add(0, selectedImage);
            }
        }

        imageList.setListData(images.toArray(new String[0]));
        if (StringUtils.isNotEmpty(selectedImage)) {
            for (int i = 0; i < images.size(); i++) {
                if (StringUtils.equals(selectImageFinal, images.get(i))) {
                    imageList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!StringUtils.isNotEmpty(sourceDirectoryTextField.getText())) {
            if (cloneFromGitRepositoryCheckBox.isSelected()) {
                return new ValidationInfo("Must specify parent directory for git repository", sourceDirectoryTextField);
            } else {
                return new ValidationInfo("Must specify source directory", sourceDirectoryTextField);
            }
        }
        if (cloneFromGitRepositoryCheckBox.isSelected()
                && !StringUtils.isNotEmpty(gitUrlTextField.getText())) {
            return new ValidationInfo("Must specify git url", gitUrlTextField);
        }
        if (containerList.getSelectedValuesList().size() == 0) {
            return new ValidationInfo("Must select a container", containerList);
        }
        if (imageList.getSelectedValuesList().size() == 0) {
            return new ValidationInfo("Must select an image", imageList);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        sourceDirectory = Paths.get(sourceDirectoryTextField.getText());
        cloneFromGit = cloneFromGitRepositoryCheckBox.isSelected();
        gitUrl = gitUrlTextField.getText();
        selectedContainer = containerList.getSelectedValue();
        selectedImage = imageList.getSelectedValue();
        super.doOKAction();
    }
}
