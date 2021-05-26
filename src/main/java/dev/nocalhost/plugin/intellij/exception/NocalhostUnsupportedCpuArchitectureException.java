package dev.nocalhost.plugin.intellij.exception;

public class NocalhostUnsupportedCpuArchitectureException extends RuntimeException {
    private String arch;

    public NocalhostUnsupportedCpuArchitectureException(String arch) {
        this.arch = arch;
    }

    @Override
    public String getMessage() {
        return "Unsupported CPU architecture: " + arch;
    }
}
