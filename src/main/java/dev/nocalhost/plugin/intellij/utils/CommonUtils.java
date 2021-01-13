package dev.nocalhost.plugin.intellij.utils;

import com.google.inject.Inject;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;

import org.apache.commons.lang3.StringUtils;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;

public final class CommonUtils {

    @Inject
    private Logger log;

    public String getErrorTextFromException(Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            message = "(No exception message available)";
            log.error(message, t);
        }
        return message;
    }

    public static DevModeService findDevModeService(int applicationId, int devSpaceId, String name) {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        for (DevModeService dms : nocalhostSettings.getStartedDevModeService()) {
            if (dms.getApplicationId() == applicationId && dms.getDevSpaceId() == devSpaceId && StringUtils.equals(dms.getName(), name)) {
                return dms;
            }
        }
        return null;
    }
}
