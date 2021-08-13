package dev.nocalhost.plugin.intellij.exception;

import lombok.Getter;

public class NhctlCommandException extends NocalhostExecuteCmdException {
    @Getter
    private String errorOutput;

    public NhctlCommandException(String cmd, int exitCode, String msg, String errorOutput) {
        super(cmd, exitCode, msg);
        this.errorOutput = errorOutput;
    }
}
