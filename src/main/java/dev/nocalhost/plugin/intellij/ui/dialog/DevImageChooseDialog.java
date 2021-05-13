package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;

import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.*;

public class DevImageChooseDialog extends DialogWrapper {
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

    private JPanel contentPane;
    private JBList<String> imageList;

    private String selectedImage;

    public DevImageChooseDialog(Project project) {
        super(project, true);
        setTitle("Select DevMode Image");
        imageList.setVisibleRowCount(10);
        imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        imageList.setListData(DEV_MODE_IMAGES.toArray(new String[0]));
        imageList.setSelectedIndex(0);
        imageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
                    doOKAction();
                }
            }
        });
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        selectedImage = imageList.getSelectedValue();
        super.doOKAction();
    }

    public String getSelectedImage() {
        return selectedImage;
    }
}
