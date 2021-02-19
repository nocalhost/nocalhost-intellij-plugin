package dev.nocalhost.plugin.intellij.exception;

public class NocalhostGitException extends Exception {
    private final int exitCode;
    private final String msg;
    public NocalhostGitException(int exitCode, String errorOutputAsJoinedString) {
        this.exitCode = exitCode;
        this.msg = errorOutputAsJoinedString;
    }

    @Override
    public String getMessage() {
        return String.format("Nocalhost Git error, exitCode: [%d]\n%s", exitCode, msg);
    }
}
