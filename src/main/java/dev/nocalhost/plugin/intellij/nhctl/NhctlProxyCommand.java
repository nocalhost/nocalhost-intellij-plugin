package dev.nocalhost.plugin.intellij.nhctl;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.nocalhost.plugin.intellij.ui.dialog.SudoPasswordDialog;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
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
    protected void consume(@NotNull Process process) {
        if (SystemInfo.isWindows) {
            return;
        }

        var output = "";
        var reader = new InputStreamReader(process.getErrorStream(), Charsets.UTF_8);
        try (var br = new BufferedReader(reader)) {
            while ((output = br.readLine()) != null && isSudo(output)) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    var dialog = new SudoPasswordDialog(project, NhctlUtil.binaryPath());
                    if (dialog.showAndGet()) {
                        inject(process, dialog.getPassword());
                    }
                });
            }
        } catch (IOException ex) {
            // ignore
        }
    }

    private boolean isSudo(String text) {
        return StringUtils.contains(text, "Password:")
                || StringUtils.contains(text, "[sudo] password for")
                || StringUtils.contains(text, "Sorry, try again:");
    }

    private void inject(@NotNull Process process, @NotNull String password) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var stream = process.getOutputStream();
            try (stream) {
                stream.write((password + "\n").getBytes(StandardCharsets.UTF_8));
                stream.flush();
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, "Failed to start proxy", "Error occurred while writing password to stdin", ex);
            }
        });
    }
}
