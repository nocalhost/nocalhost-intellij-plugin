package dev.nocalhost.plugin.intellij.exception;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class NocalhostConfusingException extends IOException {
    private String myDetails;

    public NocalhostConfusingException() {
    }

    public NocalhostConfusingException(String message) {
        super(message);
    }

    public NocalhostConfusingException(String message, Throwable cause) {
        super(message, cause);
    }

    public NocalhostConfusingException(Throwable cause) {
        super(cause);
    }

    public void setDetails(@Nullable String details) {
        myDetails = details;
    }

    @Override
    public String getMessage() {
        if (myDetails == null) {
            return super.getMessage();
        }
        else {
            return myDetails + "\n\n" + super.getMessage();
        }
    }
}
