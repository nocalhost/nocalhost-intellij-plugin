package dev.nocalhost.plugin.intellij.utils;

import com.google.inject.Inject;

import com.intellij.openapi.diagnostic.Logger;

public class CommonUtils {

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
}
