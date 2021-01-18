package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.lang3.StringUtils;

public final class NhctlUtil {
    public static String generateInstallType(String source, String originInstallType) {
        if (StringUtils.equals(source, "git") && StringUtils.equals(originInstallType, "rawManifest")) {
            return "rawManifest";
        } else if (StringUtils.equals(source, "git") && StringUtils.equals(originInstallType, "rawManifest")) {
            return "helmGit";
        } else {
            return "helmRepo";
        }
    }
}
