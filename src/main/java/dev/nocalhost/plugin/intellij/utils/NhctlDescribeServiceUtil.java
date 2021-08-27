package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.lang3.StringUtils;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;

import static dev.nocalhost.plugin.intellij.utils.Constants.DEVELOP_STATUS_STARTED;

public final class NhctlDescribeServiceUtil {
    public static boolean developStarted(NhctlDescribeService nhctlDescribeService) {
        return StringUtils.equals(nhctlDescribeService.getDevelop_status(), DEVELOP_STATUS_STARTED);
    }
}
