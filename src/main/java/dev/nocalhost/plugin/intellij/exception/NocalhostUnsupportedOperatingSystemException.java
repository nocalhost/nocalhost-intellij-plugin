package dev.nocalhost.plugin.intellij.exception;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;

public class NocalhostUnsupportedOperatingSystemException extends RuntimeException {
    private String os;

    public NocalhostUnsupportedOperatingSystemException(String os) {
        this.os = os;
    }

    @Override
    public String getMessage() {
        return NocalhostI18n.format("error.unsupportedOS", os);
    }
}
