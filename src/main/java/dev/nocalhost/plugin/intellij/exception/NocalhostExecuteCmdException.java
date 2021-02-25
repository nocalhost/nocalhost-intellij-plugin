package dev.nocalhost.plugin.intellij.exception;

public class NocalhostExecuteCmdException extends Exception {
    private final String cmd;
    private final int exitCode;
    private final String msg;

    public NocalhostExecuteCmdException(String cmd, int exitCode, String msg) {
        this.exitCode = exitCode;
        this.cmd = cmd;
        this.msg = msg;
    }


    @Override
    public String getMessage() {
        return String.format("Execute cmd: [%s], exitCode: [%d]\n%s", cmd, exitCode, msg);
    }
}
