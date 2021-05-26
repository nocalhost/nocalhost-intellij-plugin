package dev.nocalhost.plugin.intellij.exception;

public class NocalhostUnsupportedOperatingSystemException extends RuntimeException {
    private String os;

    public NocalhostUnsupportedOperatingSystemException(String os) {
        this.os = os;
    }

    @Override
    public String getMessage() {
        return "Unsupported operating system: " + os;
    }
}
