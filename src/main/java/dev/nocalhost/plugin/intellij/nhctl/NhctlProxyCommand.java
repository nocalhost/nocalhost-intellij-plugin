package dev.nocalhost.plugin.intellij.nhctl;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import dev.nocalhost.plugin.intellij.ui.dialog.SudoPasswordDialog;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.SudoUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlProxyCommand extends BaseCommand {
    private String action;
    private String workload;

    public NhctlProxyCommand(Project project) {
        super(project);
    }

    @Override
    protected List<String> compute() {
        List<String> args = Lists.newArrayList(getBinaryPath(), "vpn", action);
        if (StringUtils.isNotEmpty(workload)) {
            args.add("--workloads");
            args.add(workload);
        }
        return fulfill(args);
    }

    @Override
    protected void onError(@NotNull Process process) {
        int b;
        var output = new StringBuilder();
        var reader = new InputStreamReader(process.getErrorStream(), Charsets.UTF_8);
        try (var br = new BufferedReader(reader)) {
            while ((b = br.read()) != -1) {
                output.append((char) b);
                stderr.set(output.toString());
                if (SystemInfo.isWindows) {
                    continue;
                }
                if (StringUtils.contains(output.toString(), "Password:")) {
                    output.setLength(0);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        var dialog = new SudoPasswordDialog(project, NhctlUtil.binaryPath());
                        if (dialog.showAndGet()) {
                            SudoUtil.inputPassword(process, dialog.getPassword());
                        } else {
                            process.destroy();
                        }
                    });
                }
            }
        } catch (IOException ex) {
            // ignore
        }
    }
}
