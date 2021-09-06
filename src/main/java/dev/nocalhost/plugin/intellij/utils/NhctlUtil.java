package dev.nocalhost.plugin.intellij.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.service.NocalhostProjectService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;

public final class NhctlUtil {
    private static final Path NOCALHOST_BIN_DIR = Paths.get(System.getProperty("user.home"), ".nh", "bin");

    public static String binaryDir() {
        return NOCALHOST_BIN_DIR.toString();
    }

    public static String getName() {
        if (SystemInfo.isWindows) {
            return "nhctl.exe";
        } else {
            return "nhctl";
        }
    }

    public static String binaryPath() {
        return NOCALHOST_BIN_DIR.resolve(getName()).toAbsolutePath().toString();
    }

    public static ServiceProjectPath getDevModeService(Project project) {
        return project
                .getService(NocalhostProjectService.class)
                .getServiceProjectPath();
    }

    public static NhctlDescribeService getDescribeService(ServiceProjectPath service) throws ExecutionException {
        try {
            NhctlDescribeOptions opts = new NhctlDescribeOptions(service.getKubeConfigPath(), service.getNamespace());
            opts.setDeployment(service.getServiceName());
            opts.setType(service.getServiceType());
            return ApplicationManager
                    .getApplication()
                    .getService(NhctlCommand.class)
                    .describe(
                        service.getApplicationName(),
                        opts,
                        NhctlDescribeService.class
                    );
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
}
