package dev.nocalhost.plugin.intellij.exception;

public class NocalhostApiException extends RuntimeException {
    public NocalhostApiException() {
        super();
    }

    public NocalhostApiException(String message) {
        super(message);
    }

    public NocalhostApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public NocalhostApiException(Throwable cause) {
        super(cause);
    }

    protected NocalhostApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
