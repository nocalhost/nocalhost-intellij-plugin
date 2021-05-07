package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.components.ServiceManager;

import org.apache.commons.lang3.StringUtils;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;

public final class NhctlUtil {
    private static final String NHCTL_COMMAND = "nhctl";

    public static String binaryPath() {
        NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        return StringUtils.isNotEmpty(nocalhostSettings.getNhctlBinary())
                ? nocalhostSettings.getNhctlBinary() : NHCTL_COMMAND;
    }
}
