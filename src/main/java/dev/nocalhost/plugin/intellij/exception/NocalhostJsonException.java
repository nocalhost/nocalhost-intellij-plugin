package dev.nocalhost.plugin.intellij.exception;

public class NocalhostJsonException extends NocalhostConfusingException {
    public NocalhostJsonException() {
    }

    public NocalhostJsonException(String message) {
        super(message);
    }

    public NocalhostJsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public NocalhostJsonException(Throwable cause) {
        super(cause);
    }
}
