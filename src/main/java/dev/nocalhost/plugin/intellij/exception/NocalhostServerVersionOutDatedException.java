package dev.nocalhost.plugin.intellij.exception;

import java.text.MessageFormat;

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
        return MessageFormat.format(
                "Server [{0}] version [{1}] is lower than required minimal version [{2}]",
                server,
                currentVersion,
                requiredMinimalVersion);
    }
}
