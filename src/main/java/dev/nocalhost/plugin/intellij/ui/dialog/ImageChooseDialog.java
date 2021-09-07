package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.utils.TextUiUtil;
import lombok.Getter;

public class ImageChooseDialog extends DialogWrapper {
    private static final List<String> DEV_MODE_IMAGES = List.of(
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/java:11",
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/ruby:3.0",
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/node:14",
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/python:3.9",
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/golang:1.16",
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/perl:latest",
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/rust:latest",
            "nocalhost-docker.pkg.coding.net/nocalhost/dev-images/php:latest"
    );

    private JPanel contentPane;
    private JBRadioButton specifyRadioButton;
    private JBTextField imageTextField;
    private JBRadioButton selectRadioButton;
    private JBList<String> imageList;

    @Getter
    private String selectedImage;

    public ImageChooseDialog(@Nullable Project project) {
        super(project, true);

        setTitle("Select Dev Mode Image");

        TextUiUtil.setCutCopyPastePopup(imageTextField);

        imageList.setListData(DEV_MODE_IMAGES.toArray(new String[0]));

        ButtonGroup group = new ButtonGroup();
        group.add(specifyRadioButton);
        group.add(selectRadioButton);

        specifyRadioButton.addChangeListener(event -> updateComponents());
        selectRadioButton.addChangeListener(event -> updateComponents());

        specifyRadioButton.setSelected(true);

        init();
    }

    private void updateComponents() {
        imageTextField.setEnabled(specifyRadioButton.isSelected());
        imageList.setEnabled(selectRadioButton.isSelected());
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (specifyRadioButton.isSelected() && !StringUtils.isNotEmpty(imageTextField.getText())) {
            return new ValidationInfo("Must specify an image", imageTextField);
        }
        if (selectRadioButton.isSelected() && !StringUtils.isNotEmpty(imageList.getSelectedValue())) {
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
        if (specifyRadioButton.isSelected()) {
            selectedImage = imageTextField.getText();
        }
        if (selectRadioButton.isSelected()) {
            selectedImage = imageList.getSelectedValue();
        }
        super.doOKAction();
    }
}
