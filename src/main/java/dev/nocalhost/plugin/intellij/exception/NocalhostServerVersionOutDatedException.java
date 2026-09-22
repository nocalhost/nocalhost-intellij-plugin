package dev.nocalhost.plugin.intellij.exception;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;

public class NocalhostServerVersionOutDatedException extends Exception {
    private String server;
    private String currentVersion;
    private String requiredMinimalVersion;

    public NocalhostServerVersionOutDatedException(String server, String currentVersion, String requiredMinimalVersion) {
        this.server = server;
        this.currentVersion = currentVersion;
        this.requiredMinimalVersion = requiredMinimalVersion;
    }


    @Override
    public String getMessage() {
        return NocalhostI18n.format(
                "error.serverVersionOutDated",
                server,
                currentVersion,
                requiredMinimalVersion);
    }
}
