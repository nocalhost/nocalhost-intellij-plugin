package dev.nocalhost.plugin.intellij.helpers;

import com.intellij.openapi.components.ServiceManager;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeApplication;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;

public final class NhctlHelper {
    public static String generateInstallType(DevSpace.Context context) {
        String source = context.getSource();
        String originInstallType = context.getInstallType();
        if (StringUtils.equals(source, "git")
                && StringUtils.equals(originInstallType, "rawManifest")) {
            return "rawManifest";
        } else if (StringUtils.equals(source, "git")
                && StringUtils.equals(originInstallType, "helm_chart")) {
            return "helmGit";
        } else if (StringUtils.equals(source, "local")) {
            return originInstallType;
        } else {
            return "helmRepo";
        }
    }

    public static boolean isApplicationInstalled(DevSpace devSpace) throws IOException, InterruptedException {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final String applicationName = devSpace.getContext().getApplicationName();

        NhctlDescribeOptions opts = new NhctlDescribeOptions(devSpace);

        try {
            NhctlDescribeApplication nhctlDescribeApplication = nhctlCommand.describe(applicationName, opts, NhctlDescribeApplication.class);
            return nhctlDescribeApplication.isInstalled();
        } catch (NocalhostExecuteCmdException e) {
            return false;
        } catch (InterruptedException | IOException e) {
            throw e;
        }
    }
}
